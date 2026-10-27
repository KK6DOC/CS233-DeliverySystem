import java.io.IOException;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;

public final class FileManager implements AutoCloseable {
    private final Path directory;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private String actor = "system";
    private String actorRole = "System";

    // Opens the data folder and keeps two copies of the app from editing it at once.
    public FileManager(Path directory, Path startingMenu) throws IOException {
        this.directory = directory.toAbsolutePath();
        Files.createDirectories(this.directory);
        lockChannel = FileChannel.open(this.directory.resolve(".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = lockChannel.tryLock();
        } catch (OverlappingFileLockException ex) {
            acquired = null;
        } catch (IOException ex) {
            lockChannel.close();
            throw ex;
        }
        if (acquired == null) {
            lockChannel.close();
            throw new IOException("This data folder is already open in another copy of the app.");
        }
        lock = acquired;
        try {
            initializeFiles(startingMenu);
        } catch (IOException | RuntimeException ex) {
            close();
            throw ex;
        }
    }

    // Creates demo accounts only when none of the data files exist yet.
    private void initializeFiles(Path startingMenu) throws IOException {
        String[] names = {"Menu.txt", "customers.txt", "drivers.txt", "admins.txt", "orders.txt", "activity.txt"};
        int existing = 0;
        for (String name : names) {
            if (Files.exists(directory.resolve(name))) {
                existing++;
            }
        }
        if (existing == names.length) {
            return;
        }
        if (existing != 0) {
            throw new IOException("Some data files are missing in " + directory + ". Restore them before starting.");
        }
        Menu starting = readMenu(startingMenu);
        Files.writeString(directory.resolve("activity.txt"), "# UTC time|role|username|event|details\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        ArrayList<User> users = new ArrayList<>();
        users.add(new Admin("admin", PasswordUtil.hash("admin123"), "admin@example.com"));
        users.add(new Customer("customer", PasswordUtil.hash("customer123"), "customer@example.com"));
        users.add(new Driver("driver1", PasswordUtil.hash("driver123"), "driver1@example.com"));
        users.add(new Driver("driver2", PasswordUtil.hash("driver123"), "driver2@example.com"));
        saveMenu(starting);
        saveUsers(users, "Admin");
        saveUsers(users, "Customer");
        saveUsers(users, "Driver");
        writeFile("orders.txt", "# id|customer|status|driver|rating|driver ratings|item count|name|price...\n");
    }

    // Loads food names and prices from the working menu file.
    public Menu loadMenu() throws IOException {
        Menu menu = readMenu(directory.resolve("Menu.txt"));
        log("READ", "Loaded Menu.txt: " + menu.getItems().size() + " menu items.");
        return menu;
    }

    // Reads one item per line and also accepts the original quoted sample names.
    private Menu readMenu(Path path) throws IOException {
        Menu menu = new Menu();
        int lineNumber = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            try {
                int comma = line.lastIndexOf(',');
                if (comma < 1) {
                    throw new IllegalArgumentException("Expected a name and price separated by a comma.");
                }
                String name = line.substring(0, comma).trim();
                if (name.length() >= 2 && ((name.startsWith("'") && name.endsWith("'"))
                        || (name.startsWith("\"") && name.endsWith("\"")))) {
                    name = name.substring(1, name.length() - 1);
                }
                menu.addMenuItem(new MenuItem(name, new BigDecimal(line.substring(comma + 1).trim())));
            } catch (RuntimeException ex) {
                throw badRecord(path, lineNumber, ex);
            }
        }
        return menu;
    }

    // Saves admin menu changes without changing prices inside old orders.
    public void saveMenu(Menu menu) throws IOException {
        StringBuilder text = new StringBuilder();
        for (MenuItem item : menu.getItems()) {
            text.append(item.getName()).append(", ").append(item.getPrice()).append('\n');
        }
        writeFile("Menu.txt", text.toString());
    }

    // Loads all three user files into one username lookup.
    public LinkedHashMap<String, User> loadUsers() throws IOException {
        LinkedHashMap<String, User> users = new LinkedHashMap<>();
        for (String role : new String[]{"Admin", "Customer", "Driver"}) {
            Path path = directory.resolve(userFile(role));
            int lineNumber = 0;
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                try {
                    String[] parts = line.split("\\|", -1);
                    if (parts.length != (role.equals("Driver") ? 5 : 3)) {
                        throw new IllegalArgumentException("The user record has the wrong number of fields.");
                    }
                    User user = DeliverySystem.createUser(role, parts[0], parts[1], parts[2]);
                    if (user instanceof Driver driver) {
                        if (!parts[3].equals("true") && !parts[3].equals("false")) {
                            throw new IllegalArgumentException("Driver availability must be true or false.");
                        }
                        driver.setAvailability(Boolean.parseBoolean(parts[3]));
                        driver.restoreRatings(parseRatings(parts[4]));
                    }
                    if (users.putIfAbsent(user.getUsername(), user) != null) {
                        throw new IllegalArgumentException("There are duplicate usernames.");
                    }
                } catch (RuntimeException ex) {
                    throw badRecord(path, lineNumber, ex);
                }
            }
        }
        log("READ", "Loaded admins.txt, customers.txt, and drivers.txt.");
        return users;
    }

    // Writes one role's file, including ratings and availability for drivers.
    public void saveUsers(Collection<User> users, String role) throws IOException {
        StringBuilder text = new StringBuilder("# username|password hash|email");
        if (role.equals("Driver")) {
            text.append("|available|ratings (oldest first)");
        }
        text.append('\n');
        for (User user : users) {
            if (!user.getRole().equals(role)) {
                continue;
            }
            text.append(user.getUsername()).append('|').append(user.getPasswordHash())
                    .append('|').append(user.getEmail());
            if (user instanceof Driver driver) {
                text.append('|').append(driver.getAvailability()).append('|')
                        .append(formatRatings(driver.getRatings()));
            }
            text.append('\n');
        }
        writeFile(userFile(role), text.toString());
    }

    // Replays the transaction log and keeps each order's latest saved version.
    public LinkedHashMap<Integer, Order> loadOrders(LinkedHashMap<String, User> users) throws IOException {
        LinkedHashMap<Integer, Order> orders = new LinkedHashMap<>();
        LinkedHashMap<String, Deque<Integer>> ratingSnapshots = new LinkedHashMap<>();
        Path path = directory.resolve("orders.txt");
        int lineNumber = 0;
        int highestId = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            try {
                String[] parts = line.split("\\|", -1);
                int count = Integer.parseInt(parts[6]);
                if (count < 1 || count > 100 || parts.length != 7 + count * 2) {
                    throw new IllegalArgumentException("The saved item count is not valid.");
                }
                ArrayList<MenuItem> items = new ArrayList<>();
                for (int i = 7; i < parts.length; i += 2) {
                    items.add(new MenuItem(parts[i], new BigDecimal(parts[i + 1])));
                }
                Order order = new Order(Integer.parseInt(parts[0]), parts[1], items,
                        Order.Status.valueOf(parts[2]), parts[3], Integer.parseInt(parts[4]));
                if (!(users.get(order.getCustomer()) instanceof Customer)
                        || (!order.getDriver().isEmpty() && !(users.get(order.getDriver()) instanceof Driver))) {
                    throw new IllegalArgumentException("The order refers to a missing customer or driver.");
                }
                Order previous = orders.get(order.getOrderId());
                if (previous == null) {
                    if (order.getStatus() != Order.Status.PLACED || order.getOrderId() <= highestId) {
                        throw new IllegalArgumentException("A new order must start as placed with a new number.");
                    }
                    highestId = order.getOrderId();
                } else {
                    validateTransition(previous, order);
                }
                Deque<Integer> savedRatings = parseRatings(parts[5]);
                if (order.getRating() > 0) {
                    if (savedRatings.isEmpty() || savedRatings.getLast() != order.getRating()) {
                        throw new IllegalArgumentException("The order's rating does not match the driver's ratings.");
                    }
                    ratingSnapshots.put(order.getDriver(), savedRatings);
                } else if (!savedRatings.isEmpty()) {
                    throw new IllegalArgumentException("An unrated order cannot contain a rating snapshot.");
                }
                orders.put(order.getOrderId(), order);
            } catch (RuntimeException ex) {
                throw badRecord(path, lineNumber, ex);
            }
        }
        for (String username : ratingSnapshots.keySet()) {
            ((Driver) users.get(username)).restoreRatings(ratingSnapshots.get(username));
        }
        log("READ", "Replayed orders.txt: " + orders.size() + " orders restored.");
        return orders;
    }

    // Checks that saved updates do not change ownership, prices, or skip delivery steps.
    private void validateTransition(Order previous, Order next) {
        if (!previous.getCustomer().equals(next.getCustomer())
                || previous.getItems().size() != next.getItems().size()) {
            throw new IllegalArgumentException("An existing order's customer or items changed.");
        }
        for (int i = 0; i < previous.getItems().size(); i++) {
            MenuItem first = previous.getItems().get(i);
            MenuItem second = next.getItems().get(i);
            if (!first.getName().equals(second.getName()) || !first.getPrice().equals(second.getPrice())) {
                throw new IllegalArgumentException("An existing order's item or price changed.");
            }
        }
        boolean valid = switch (previous.getStatus()) {
            case PLACED -> next.getStatus() == Order.Status.ACCEPTED || next.getStatus() == Order.Status.CANCELLED;
            case ACCEPTED -> next.getStatus() == Order.Status.IN_PROGRESS;
            case IN_PROGRESS -> next.getStatus() == Order.Status.DELIVERED;
            case DELIVERED -> previous.getRating() == 0 && next.getStatus() == Order.Status.DELIVERED
                    && next.getRating() > 0;
            case CANCELLED -> false;
        };
        if (!valid || (!previous.getDriver().isEmpty() && !previous.getDriver().equals(next.getDriver()))) {
            throw new IllegalArgumentException("The saved order has an invalid status change.");
        }
    }

    // Appends a full order snapshot each time an order is placed or updated.
    public void appendOrder(Order order, Deque<Integer> ratings) throws IOException {
        StringBuilder text = new StringBuilder();
        text.append(order.getOrderId()).append('|').append(order.getCustomer()).append('|')
                .append(order.getStatus()).append('|').append(order.getDriver()).append('|')
                .append(order.getRating()).append('|').append(formatRatings(ratings)).append('|')
                .append(order.getItems().size());
        for (MenuItem item : order.getItems()) {
            text.append('|').append(item.getName()).append('|').append(item.getPrice());
        }
        text.append('\n');
        Files.writeString(directory.resolve("orders.txt"), text, StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
        log("SAVE", "Appended order #" + order.getOrderId() + " to orders.txt: " + order.getStatus()
                + ", total $" + order.calculateTotalCost() + ".");
    }

    // Reads a driver's saved ratings in the order they were received.
    private Deque<Integer> parseRatings(String text) {
        Deque<Integer> ratings = new ArrayDeque<>();
        if (!text.isEmpty()) {
            for (String value : text.split(",", -1)) {
                int rating = Integer.parseInt(value);
                if (rating < 1 || rating > 5 || ratings.size() == 10) {
                    throw new IllegalArgumentException("Saved ratings must be 1 to 5, with at most ten ratings.");
                }
                ratings.addLast(rating);
            }
        }
        return ratings;
    }

    // Joins ratings without changing their oldest-to-newest order.
    private String formatRatings(Deque<Integer> ratings) {
        StringBuilder text = new StringBuilder();
        for (int rating : ratings) {
            if (!text.isEmpty()) {
                text.append(',');
            }
            text.append(rating);
        }
        return text.toString();
    }

    // Chooses the file for a user role.
    private String userFile(String role) {
        return switch (role) {
            case "Admin" -> "admins.txt";
            case "Customer" -> "customers.txt";
            case "Driver" -> "drivers.txt";
            default -> throw new IllegalArgumentException("Unknown user role.");
        };
    }

    // Replaces a text file only after its new contents have been written.
    private void writeFile(String name, String text) throws IOException {
        Path temporary = Files.createTempFile(directory, name, ".tmp");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            log("SAVE", "Saved " + name + ".");
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    // Includes the file and line number when saved data cannot be read.
    private IOException badRecord(Path path, int line, RuntimeException cause) {
        return new IOException("Cannot read " + path.getFileName() + " at line " + line + ": " + cause.getMessage(), cause);
    }

    // Tags later file activity with the user who requested it.
    public void setActor(User user) {
        actor = user == null ? "system" : user.getUsername();
        actorRole = user == null ? "System" : user.getRole();
    }

    // Records activity without saving credentials or full user-file contents.
    public void log(String event, String details) throws IOException {
        String safeDetails = details.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
        String line = Instant.now() + "|" + actorRole + "|" + actor + "|" + event + "|" + safeDetails + "\n";
        Files.writeString(directory.resolve("activity.txt"), line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    // Returns recent activity, limited to the current user unless they are an admin.
    public ArrayList<ActivityEntry> loadActivity(User user) throws IOException {
        ArrayDeque<ActivityEntry> recent = new ArrayDeque<>();
        Path path = directory.resolve("activity.txt");
        int lineNumber = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            if (parts.length != 5) {
                throw new IOException("Cannot read activity.txt at line " + lineNumber + ".");
            }
            if (user instanceof Admin || parts[2].equals(user.getUsername())) {
                if (recent.size() == 200) {
                    recent.removeFirst();
                }
                recent.addLast(new ActivityEntry(parts[0], parts[1], parts[2], parts[3], parts[4]));
            }
        }
        return new ArrayList<>(recent);
    }

    // Releases the data folder when the app closes.
    @Override
    public void close() throws IOException {
        try {
            if (lock.isValid()) {
                lock.release();
            }
        } finally {
            lockChannel.close();
        }
    }
}
