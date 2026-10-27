import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class DeliverySystem implements AutoCloseable {
    private final FileManager files;
    private Menu menu;
    private final LinkedHashMap<String, User> users;
    private final LinkedHashMap<Integer, Order> allOrders;
    private final Queue<Order> ordersQueue = new ArrayDeque<>();
    private final PriorityQueue<Driver> readyDrivers = new PriorityQueue<>();
    private User currentUser;
    private int nextOrderNum = 1;

    // Loads the saved system and restores waiting orders and busy drivers.
    public DeliverySystem(Path directory, Path startingMenu) throws IOException {
        files = new FileManager(directory, startingMenu);
        try {
            menu = files.loadMenu();
            users = files.loadUsers();
            allOrders = files.loadOrders(users);
            for (User user : users.values()) {
                if (user instanceof Driver driver) {
                    driver.setAvailability(true);
                }
            }
            for (Order order : allOrders.values()) {
                if (order.getOrderId() == Integer.MAX_VALUE) {
                    throw new IOException("No more order numbers are available.");
                }
                nextOrderNum = Math.max(nextOrderNum, order.getOrderId() + 1);
                if (order.getStatus() == Order.Status.PLACED) {
                    ordersQueue.offer(order);
                } else if (order.getStatus() == Order.Status.ACCEPTED || order.getStatus() == Order.Status.IN_PROGRESS) {
                    Driver driver = (Driver) users.get(order.getDriver());
                    if (!driver.getAvailability()) {
                        throw new IOException("A driver has more than one active order in the saved data.");
                    }
                    driver.setAvailability(false);
                }
            }
            for (User user : users.values()) {
                if (user instanceof Driver driver && driver.getAvailability()) {
                    readyDrivers.offer(driver);
                }
            }
            giveOrderToDriver();
        } catch (IOException | RuntimeException ex) {
            files.close();
            throw ex;
        }
    }

    // Creates the correct subclass when loading or adding a user.
    static User createUser(String role, String username, String hash, String email) {
        return switch (role) {
            case "Admin" -> new Admin(username, hash, email);
            case "Customer" -> new Customer(username, hash, email);
            case "Driver" -> new Driver(username, hash, email);
            default -> throw new IllegalArgumentException("Choose Admin, Customer, or Driver.");
        };
    }

    // Logs in one user and chooses their role from the saved account.
    public User login(String username, String password) throws IOException {
        if (currentUser != null) {
            throw new IllegalArgumentException("Log out before changing accounts.");
        }
        User user = users.get(User.normalizeUsername(username));
        if (user == null || !user.login(password)) {
            throw new IllegalArgumentException("Username or password is incorrect.");
        }
        currentUser = user;
        files.setActor(user);
        files.log("LOGIN", "Signed in to the " + user.getRole() + " dashboard.");
        return currentUser;
    }

    // Ends the current login session.
    public void logout() throws IOException {
        if (currentUser != null) {
            files.log("LOGOUT", "Signed out.");
        }
        currentUser = null;
        files.setActor(null);
    }

    // Returns the logged-in user for the dashboard.
    public User getCurrentUser() {
        return currentUser;
    }

    // Allows customer sign-up, while only admins can create other roles.
    public void register(String role, String username, String password, String email) throws IOException {
        if (currentUser != null || !role.equals("Customer")) {
            requireRole(Admin.class);
        }
        String key = User.normalizeUsername(username);
        if (users.containsKey(key)) {
            throw new IllegalArgumentException("That username is already taken.");
        }
        User user = createUser(role, key, PasswordUtil.hash(password), email);
        files.log("REQUEST", "register: create " + role + " account " + key + ".");
        LinkedHashMap<String, User> proposed = new LinkedHashMap<>(users);
        proposed.put(key, user);
        files.saveUsers(proposed.values(), role);
        users.put(key, user);
        if (user instanceof Driver driver) {
            readyDrivers.offer(driver);
            giveOrderToDriver();
        }
    }

    // Updates the current user's email and optionally their password.
    public void changeInfo(String email, String newPassword) throws IOException {
        requireLogin();
        String hash = newPassword.isEmpty() ? currentUser.getPasswordHash() : PasswordUtil.hash(newPassword);
        User replacement = createUser(currentUser.getRole(), currentUser.getUsername(), hash, email);
        if (replacement instanceof Driver next && currentUser instanceof Driver old) {
            next.restoreRatings(old.getRatings());
            next.setAvailability(old.getAvailability());
        }
        LinkedHashMap<String, User> proposed = new LinkedHashMap<>(users);
        proposed.put(replacement.getUsername(), replacement);
        files.log("REQUEST", "changeInfo: update profile" + (newPassword.isEmpty() ? "." : " and password."));
        files.saveUsers(proposed.values(), replacement.getRole());
        if (currentUser instanceof Driver driver) {
            readyDrivers.remove(driver);
        }
        currentUser = replacement;
        users.put(replacement.getUsername(), replacement);
        if (replacement instanceof Driver driver && driver.getAvailability()) {
            readyDrivers.offer(driver);
        }
    }

    // Returns a list of immutable menu items for either interface.
    public ArrayList<MenuItem> getMenuItems() {
        requireLogin();
        return menu.getItems();
    }

    // Adds an item after checking that an admin is logged in.
    public void addMenuItem(String name, BigDecimal price) throws IOException {
        requireRole(Admin.class);
        Menu proposed = copyMenu();
        proposed.addMenuItem(new MenuItem(name, price));
        files.log("REQUEST", "addMenuItem: " + name + ", $" + price + ".");
        files.saveMenu(proposed);
        menu = proposed;
    }

    // Changes a menu price without changing any existing orders.
    public void updateMenu(String name, BigDecimal price) throws IOException {
        requireRole(Admin.class);
        Menu proposed = copyMenu();
        proposed.updateMenu(new MenuItem(name, price));
        files.log("REQUEST", "updateMenu: " + name + ", $" + price + ".");
        files.saveMenu(proposed);
        menu = proposed;
    }

    // Removes an item from the menu.
    public void removeMenuItem(String name) throws IOException {
        requireRole(Admin.class);
        Menu proposed = copyMenu();
        proposed.removeMenuItem(name);
        files.log("REQUEST", "removeMenuItem: " + name + ".");
        files.saveMenu(proposed);
        menu = proposed;
    }

    // Makes a working menu copy before saving a change.
    private Menu copyMenu() {
        Menu copy = new Menu();
        for (MenuItem item : menu.getItems()) {
            copy.addMenuItem(item);
        }
        return copy;
    }

    // Saves a new order, then tries to assign waiting orders in FIFO order.
    public Order placeOrder(List<String> foodNames) throws IOException {
        requireRole(Customer.class);
        if (foodNames == null || foodNames.isEmpty() || foodNames.size() > 100) {
            throw new IllegalArgumentException("Choose 1 to 100 items for the order.");
        }
        if (nextOrderNum == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("No more order numbers are available.");
        }
        ArrayList<MenuItem> items = new ArrayList<>();
        for (String name : foodNames) {
            items.add(menu.getItem(name));
        }
        Order order = new Order(nextOrderNum, currentUser.getUsername(), items);
        files.log("REQUEST", "placeOrder: items=" + foodNames + ", total $" + order.calculateTotalCost() + ".");
        files.appendOrder(order, new ArrayDeque<>());
        nextOrderNum++;
        allOrders.put(order.getOrderId(), order);
        ordersQueue.offer(order);
        giveOrderToDriver();
        return allOrders.get(order.getOrderId()).copy();
    }

    // Assigns the oldest waiting order to the highest-rated available driver.
    private void giveOrderToDriver() throws IOException {
        while (!ordersQueue.isEmpty() && !readyDrivers.isEmpty()) {
            Order order = ordersQueue.peek().copy();
            Driver driver = readyDrivers.peek();
            order.assignDriver(driver.getUsername());
            files.log("ASSIGN", "Assigning oldest waiting order #" + order.getOrderId() + " to "
                    + driver.getUsername() + " (average " + driver.getAverageRating() + ").");
            files.appendOrder(order, new ArrayDeque<>());
            allOrders.put(order.getOrderId(), order);
            ordersQueue.remove();
            readyDrivers.remove();
            driver.setAvailability(false);
            files.saveUsers(users.values(), "Driver");
        }
    }

    // Returns the orders that the current customer or driver is allowed to see.
    public ArrayList<Order> getMyOrders() {
        requireLogin();
        ArrayList<Order> result = new ArrayList<>();
        for (Order order : allOrders.values()) {
            boolean customerOrder = currentUser instanceof Customer
                    && order.getCustomer().equals(currentUser.getUsername());
            boolean driverOrder = currentUser instanceof Driver
                    && order.getDriver().equals(currentUser.getUsername());
            if (customerOrder || driverOrder) {
                result.add(order.copy());
            }
        }
        return result;
    }

    // Returns all orders for the admin dashboard.
    public ArrayList<Order> getAllOrders() {
        requireRole(Admin.class);
        ArrayList<Order> result = new ArrayList<>();
        for (Order order : allOrders.values()) {
            result.add(order.copy());
        }
        return result;
    }

    // Returns driver copies so a screen cannot change the assignment pool.
    public ArrayList<Driver> getDrivers() {
        requireRole(Admin.class);
        ArrayList<Driver> result = new ArrayList<>();
        for (User user : users.values()) {
            if (user instanceof Driver driver) {
                Driver copy = new Driver(driver.getUsername(), driver.getPasswordHash(), driver.getEmail());
                copy.restoreRatings(driver.getRatings());
                copy.setAvailability(driver.getAvailability());
                result.add(copy);
            }
        }
        result.sort(null);
        return result;
    }

    // Cancels a customer's own order while it is waiting for a driver.
    public void cancelOrder(int id) throws IOException {
        requireRole(Customer.class);
        Order order = ownedOrder(id, false).copy();
        order.cancel();
        files.log("REQUEST", "cancelOrder: #" + id + ".");
        files.appendOrder(order, new ArrayDeque<>());
        allOrders.put(id, order);
        ordersQueue.removeIf(waiting -> waiting.getOrderId() == id);
    }

    // Lets the assigned driver start an accepted delivery.
    public void startDelivery(int id) throws IOException {
        requireRole(Driver.class);
        Order order = ownedOrder(id, true).copy();
        order.startDelivery();
        files.log("REQUEST", "startDelivery: #" + id + ".");
        files.appendOrder(order, new ArrayDeque<>());
        allOrders.put(id, order);
    }

    // Finishes a delivery and returns the driver to the pool for the next order.
    public void completeDelivery(int id) throws IOException {
        requireRole(Driver.class);
        Order order = ownedOrder(id, true).copy();
        order.completeDelivery();
        files.log("REQUEST", "completeDelivery: #" + id + ".");
        files.appendOrder(order, new ArrayDeque<>());
        allOrders.put(id, order);
        Driver driver = (Driver) currentUser;
        driver.setAvailability(true);
        readyDrivers.offer(driver);
        files.saveUsers(users.values(), "Driver");
        giveOrderToDriver();
    }

    // Saves one rating from the customer who placed a delivered order.
    public void rateDriver(int id, int rating) throws IOException {
        requireRole(Customer.class);
        Order order = ownedOrder(id, false).copy();
        order.rate(rating);
        Driver driver = (Driver) users.get(order.getDriver());
        Deque<Integer> ratings = driver.getRatings();
        if (ratings.size() == 10) {
            ratings.removeFirst();
        }
        ratings.addLast(rating);
        files.log("REQUEST", "rateDriver: order #" + id + ", " + rating + "/5.");
        files.appendOrder(order, ratings);
        allOrders.put(id, order);
        readyDrivers.remove(driver);
        driver.restoreRatings(ratings);
        if (driver.getAvailability()) {
            readyDrivers.offer(driver);
        }
        files.saveUsers(users.values(), "Driver");
    }

    // Gives each dashboard its own real backend and file activity.
    public ArrayList<ActivityEntry> getActivity() throws IOException {
        requireLogin();
        return files.loadActivity(currentUser);
    }

    // Checks both the order number and the current user's ownership.
    private Order ownedOrder(int id, boolean driver) {
        Order order = allOrders.get(id);
        if (order == null || !(driver ? order.getDriver() : order.getCustomer()).equals(currentUser.getUsername())) {
            throw new IllegalArgumentException("That order does not belong to your account.");
        }
        return order;
    }

    // Stops dashboard actions when nobody is logged in.
    private void requireLogin() {
        if (currentUser == null) {
            throw new IllegalArgumentException("Log in first.");
        }
    }

    // Checks the role in the system as well as in the interface.
    private void requireRole(Class<? extends User> role) {
        requireLogin();
        if (!role.isInstance(currentUser)) {
            throw new IllegalArgumentException("Your account cannot use that option.");
        }
    }

    // Releases the data folder when the program exits.
    @Override
    public void close() throws IOException {
        files.close();
    }
}
