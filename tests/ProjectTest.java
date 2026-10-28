import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProjectTest {
    private static int checks;

    @FunctionalInterface
    private interface CheckedAction {
        // Lets a test run an operation that may fail while reading or saving files.
        void run() throws Exception;
    }

    // Runs real file-backed tests without changing the app's normal data folder.
    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("cs233-tests-");
        try {
            orderFlow(temporary.resolve("orders"));
            recentRatings(temporary.resolve("ratings"));
            updatedDriverPriority(temporary.resolve("priority"));
            waitingAfterRestart(temporary.resolve("waiting"));
            interruptedDriverSave(temporary.resolve("interrupted"));
            activityHistory(temporary.resolve("activity"));
            menuNamesAfterRestart(temporary.resolve("menu names"));
            invalidFiles(temporary.resolve("invalid"));
            fileLock(temporary.resolve("lock"));
            inputValidation();
            System.out.println("Passed " + checks + " checks.");
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    // Checks all roles, FIFO assignment, ownership, menu edits, logs, and restart behavior.
    private static void orderFlow(Path directory) throws Exception {
        try (DeliverySystem system = open(directory)) {
            fails(() -> system.getAllOrders(), IllegalArgumentException.class, "Login is required");
            fails(() -> system.register("Admin", "badadmin", "secret12", "bad@example.com"),
                    IllegalArgumentException.class, "Public sign-up cannot create admins");
            fails(() -> system.login("admin", "wrong"), IllegalArgumentException.class, "Wrong password rejected");
            system.register("Customer", "other", "other123", "other@example.com");
            system.login("CUSTOMER", "customer123");
            check(system.getCurrentUser() instanceof Customer, "Customer role is loaded from the account");
            check(system.getMenuItems().size() == 3, "Menu configuration loads all three foods");
            fails(() -> system.addMenuItem("Freebie", BigDecimal.ZERO), IllegalArgumentException.class,
                    "Customers cannot manage the menu");
            Order first = system.placeOrder(List.of("Hamburger", "Fries", "Drink", "Drink"));
            equal(new BigDecimal("17.96"), first.calculateTotalCost(), "Order total includes repeated items");
            equal("driver1", first.getDriver(), "Equal ratings use a deterministic username tie-break");
            Order second = system.placeOrder(List.of("Drink"));
            equal("driver2", second.getDriver(), "Busy drivers are not assigned twice");
            Order third = system.placeOrder(List.of("Fries"));
            equal(Order.Status.PLACED, third.getStatus(), "Orders wait when every driver is busy");
            Order cancelled = system.placeOrder(List.of("Drink"));
            system.cancelOrder(cancelled.getOrderId());
            Order fifth = system.placeOrder(List.of("Hamburger"));
            fails(() -> system.cancelOrder(first.getOrderId()), IllegalArgumentException.class,
                    "Accepted orders cannot be cancelled");
            fails(() -> system.rateDriver(first.getOrderId(), 5), IllegalArgumentException.class,
                    "Undelivered orders cannot be rated");
            fails(() -> system.startDelivery(first.getOrderId()), IllegalArgumentException.class,
                    "Customers cannot use driver actions");
            check(system.getActivity().stream().allMatch(entry -> entry.getUsername().equals("customer")),
                    "Customers see only their own activity");
            check(system.getActivity().stream().anyMatch(entry -> entry.getDetails().contains("orders.txt")),
                    "Activity shows actual order-file writes");
            first.getItems().clear();
            equal(4, system.getMyOrders().get(0).getItems().size(), "Returned item lists are defensive copies");
            system.logout();
            system.login("other", "other123");
            equal(0, system.getMyOrders().size(), "A customer cannot see another customer's orders");
            fails(() -> system.cancelOrder(third.getOrderId()), IllegalArgumentException.class,
                    "A customer cannot cancel another customer's order");
            system.logout();
            system.login("driver2", "driver123");
            fails(() -> system.startDelivery(first.getOrderId()), IllegalArgumentException.class,
                    "Drivers cannot update another driver's delivery");
            fails(() -> system.completeDelivery(second.getOrderId()), IllegalArgumentException.class,
                    "Delivery cannot skip the in-progress step");
            system.startDelivery(second.getOrderId());
            system.completeDelivery(second.getOrderId());
            equal("driver2", find(system.getMyOrders(), third.getOrderId()).getDriver(), "The oldest waiting order goes next");
            fails(() -> system.getAllOrders(), IllegalArgumentException.class, "Drivers cannot view all customer orders");
            check(system.getActivity().stream().allMatch(entry -> entry.getUsername().equals("driver2")),
                    "Drivers see only their own activity");
            system.logout();
            system.login("driver1", "driver123");
            system.startDelivery(first.getOrderId());
            system.completeDelivery(first.getOrderId());
            equal(Order.Status.ACCEPTED, find(system.getMyOrders(), fifth.getOrderId()).getStatus(),
                    "Cancelled orders are skipped in the waiting queue");
            system.startDelivery(fifth.getOrderId());
            system.completeDelivery(fifth.getOrderId());
            system.logout();
            system.login("driver2", "driver123");
            system.startDelivery(third.getOrderId());
            system.completeDelivery(third.getOrderId());
            system.logout();
            system.login("customer", "customer123");
            system.rateDriver(first.getOrderId(), 2);
            system.rateDriver(second.getOrderId(), 5);
            fails(() -> system.rateDriver(second.getOrderId(), 1), IllegalArgumentException.class,
                    "A delivery cannot be rated twice");
            fails(() -> system.rateDriver(third.getOrderId(), 6), IllegalArgumentException.class,
                    "Out-of-range ratings are rejected");
            Order sixth = system.placeOrder(List.of("Fries"));
            equal("driver2", sixth.getDriver(), "The highest-rated available driver is chosen");
            system.rateDriver(third.getOrderId(), 1);
            Order seventh = system.placeOrder(List.of("Drink"));
            equal("driver1", seventh.getDriver(), "Rating a busy driver does not make them available");
            system.logout();
            system.login("admin", "admin123");
            system.updateMenu("Hamburger", new BigDecimal("12.50"));
            equal(new BigDecimal("17.96"), system.getAllOrders().get(0).calculateTotalCost(),
                    "Menu changes do not rewrite old order totals");
            system.addMenuItem("Onion rings", new BigDecimal("4.50"));
            fails(() -> system.addMenuItem("ONION RINGS", BigDecimal.ONE), IllegalArgumentException.class,
                    "Menu names cannot be duplicated with different capitalization");
            system.removeMenuItem("Drink");
            system.register("Admin", "helper", "helper123", "helper@example.com");
            system.register("Driver", "driver3", "driver3123", "driver3@example.com");
            check(system.getActivity().stream().anyMatch(entry -> entry.getRole().equals("System")),
                    "Admins can see startup file reads");
            check(system.getActivity().stream().anyMatch(entry -> entry.getUsername().equals("driver1")),
                    "Admins can see driver activity");
            system.changeInfo("new-admin@example.com", "changed123");
        }
        try (DeliverySystem restored = open(directory)) {
            fails(() -> restored.login("admin", "admin123"), IllegalArgumentException.class, "Old password stops working");
            restored.login("admin", "changed123");
            equal("new-admin@example.com", restored.getCurrentUser().getEmail(), "Profile changes survive restart");
            equal(7, restored.getAllOrders().size(), "All orders survive restart");
            equal(Order.Status.CANCELLED, find(restored.getAllOrders(), 4).getStatus(), "Cancelled status survives restart");
            equal(new BigDecimal("12.50"), restored.getMenuItems().stream()
                    .filter(item -> item.getName().equals("Hamburger")).findFirst().orElseThrow().getPrice(),
                    "Admin price changes survive restart");
            check(restored.getMenuItems().stream().noneMatch(item -> item.getName().equals("Drink")),
                    "Menu removal survives restart");
            check(restored.getDrivers().stream().filter(driver -> driver.getUsername().equals("driver1"))
                    .noneMatch(Driver::getAvailability), "An assigned driver stays busy after restart");
            restored.logout();
            restored.login("helper", "helper123");
            check(restored.getCurrentUser() instanceof Admin, "New admin accounts persist");
            restored.logout();
            restored.login("driver3", "driver3123");
            check(((Driver) restored.getCurrentUser()).getAvailability(), "New unassigned drivers persist as available");
            restored.logout();
            restored.login("driver2", "driver123");
            equal(List.of(5, 1), new ArrayList<>(((Driver) restored.getCurrentUser()).getRatings()),
                    "Ratings restore in the order they were received");
            restored.startDelivery(6);
        }
        try (DeliverySystem restored = open(directory)) {
            restored.login("driver2", "driver123");
            equal(Order.Status.IN_PROGRESS, find(restored.getMyOrders(), 6).getStatus(),
                    "An in-progress delivery survives another restart");
            restored.completeDelivery(6);
            check(((Driver) restored.getCurrentUser()).getAvailability(), "Completed drivers return to the available pool");
        }
        String userFile = Files.readString(directory.resolve("admins.txt"));
        check(!userFile.contains("changed123") && userFile.contains("pbkdf2$"), "User files store hashes instead of passwords");
        String activity = Files.readString(directory.resolve("activity.txt"));
        check(!activity.contains("customer123") && !activity.contains("changed123") && !activity.contains("pbkdf2$"),
                "Activity does not expose passwords or hashes");
        check(Files.readAllLines(directory.resolve("orders.txt")).size() > 8,
                "Order updates append events instead of replacing the transaction log");
    }

    // Checks the ten-rating limit, rating order, heap updates, and restored averages.
    private static void recentRatings(Path directory) throws Exception {
        try (DeliverySystem system = open(directory)) {
            for (int i = 1; i <= 11; i++) {
                system.login("customer", "customer123");
                Order order = system.placeOrder(List.of("Fries"));
                equal("driver1", order.getDriver(), "The rated driver stays first when both drivers are available");
                system.logout();
                system.login("driver1", "driver123");
                system.startDelivery(order.getOrderId());
                system.completeDelivery(order.getOrderId());
                system.logout();
                system.login("customer", "customer123");
                system.rateDriver(order.getOrderId(), i == 1 ? 1 : 5);
                system.logout();
            }
            system.login("driver1", "driver123");
            Driver driver = (Driver) system.getCurrentUser();
            equal(10, driver.getRatings().size(), "Only ten ratings are kept");
            equal(5.0, driver.getAverageRating(), "The 11th rating replaces the oldest rating in the average");
            driver.getRatings().clear();
            equal(10, driver.getRatings().size(), "Rating lists cannot be changed through getters");
        }
        try (DeliverySystem system = open(directory)) {
            system.login("driver1", "driver123");
            Driver driver = (Driver) system.getCurrentUser();
            equal(10, driver.getRatings().size(), "The rating limit survives restart");
            equal(5.0, driver.getAverageRating(), "The recalculated average survives restart");
        }
    }

    // Checks that an available driver's lower average changes their place in the heap.
    private static void updatedDriverPriority(Path directory) throws Exception {
        try (DeliverySystem system = open(directory)) {
            system.login("customer", "customer123");
            system.placeOrder(List.of("Fries"));
            system.placeOrder(List.of("Fries"));
            system.logout();
            for (int i = 1; i <= 2; i++) {
                system.login("driver" + i, "driver123");
                system.startDelivery(i);
                system.completeDelivery(i);
                system.logout();
            }
            system.login("customer", "customer123");
            system.rateDriver(1, 5);
            system.rateDriver(2, 4);
            equal("driver1", system.placeOrder(List.of("Fries")).getDriver(), "The current highest average is first");
            system.logout();
            system.login("driver1", "driver123");
            system.startDelivery(3);
            system.completeDelivery(3);
            system.logout();
            system.login("customer", "customer123");
            system.rateDriver(3, 1);
            equal("driver2", system.placeOrder(List.of("Fries")).getDriver(),
                    "An available driver is reordered after a rating lowers their average");
        }
    }

    // Checks that a restart keeps FIFO order and that adding a driver clears the next waiting order.
    private static void waitingAfterRestart(Path directory) throws Exception {
        try (DeliverySystem system = open(directory)) {
            system.login("customer", "customer123");
            for (int i = 0; i < 4; i++) {
                system.placeOrder(List.of("Drink"));
            }
        }
        try (DeliverySystem system = open(directory)) {
            system.login("customer", "customer123");
            equal(Order.Status.PLACED, find(system.getMyOrders(), 3).getStatus(), "Waiting orders survive restart");
            system.logout();
            system.login("driver2", "driver123");
            system.startDelivery(2);
            system.completeDelivery(2);
            equal(Order.Status.ACCEPTED, find(system.getMyOrders(), 3).getStatus(), "Restored waiting orders keep FIFO order");
            system.logout();
            system.login("admin", "admin123");
            system.register("Driver", "driver3", "driver3123", "driver3@example.com");
            equal("driver3", find(system.getAllOrders(), 4).getDriver(), "A newly added driver takes the next waiting order");
        }
    }

    // Checks recovery when the order log saves but the driver snapshot cannot be replaced.
    private static void interruptedDriverSave(Path directory) throws Exception {
        try (DeliverySystem system = open(directory)) {
            system.login("customer", "customer123");
            Files.move(directory.resolve("drivers.txt"), directory.resolve("drivers.saved"));
            Files.createDirectory(directory.resolve("drivers.txt"));
            fails(() -> system.placeOrder(List.of("Fries")), IOException.class, "A driver-file write failure is reported");
        }
        Files.delete(directory.resolve("drivers.txt"));
        Files.move(directory.resolve("drivers.saved"), directory.resolve("drivers.txt"));
        try (DeliverySystem system = open(directory)) {
            system.login("driver1", "driver123");
            equal(Order.Status.ACCEPTED, system.getMyOrders().get(0).getStatus(), "The successful order append survives a later save failure");
            check(!((Driver) system.getCurrentUser()).getAvailability(), "Busy state is recovered from orders even with a stale driver file");
        }
    }

    // Checks that the Activity page limit does not delete older saved events.
    private static void activityHistory(Path directory) throws Exception {
        try (DeliverySystem system = open(directory)) {
            system.login("customer", "customer123");
            for (int i = 0; i < 105; i++) {
                system.changeInfo("customer@example.com", "");
            }
            equal(200, system.getActivity().size(), "The Activity page returns the most recent 200 matching events");
            check(Files.readAllLines(directory.resolve("activity.txt")).size() > 210, "Older activity stays in the persistent file");
        }
        try (DeliverySystem system = open(directory)) {
            system.login("customer", "customer123");
            equal(200, system.getActivity().size(), "Filtered activity history survives restart");
        }
    }

    // Checks food names that could otherwise be mistaken for file comments or quoting.
    private static void menuNamesAfterRestart(Path directory) throws Exception {
        List<String> names = List.of("#1 Special", "'Chef's special'", "\"Daily special\"");
        try (DeliverySystem system = open(directory)) {
            system.login("admin", "admin123");
            for (String name : names) {
                system.addMenuItem(name, new BigDecimal("3.10"));
            }
        }
        try (DeliverySystem system = open(directory)) {
            system.login("admin", "admin123");
            for (String name : names) {
                check(system.getMenuItems().stream().anyMatch(item -> item.getName().equals(name)
                        && item.getPrice().equals(new BigDecimal("3.10"))), "A quoted or comment-like food name survives restart: " + name);
            }
        }
    }

    // Makes sure malformed or missing data is reported instead of silently reset.
    private static void invalidFiles(Path directory) throws Exception {
        try (DeliverySystem system = open(directory)) {
            system.login("customer", "customer123");
            system.placeOrder(List.of("Fries"));
        }
        String original = Files.readString(directory.resolve("orders.txt"));
        Files.writeString(directory.resolve("orders.txt"), "broken order\n", StandardOpenOption.APPEND);
        fails(() -> open(directory), IOException.class, "Malformed orders stop startup");
        check(Files.readString(directory.resolve("orders.txt")).startsWith(original), "Malformed data is not overwritten");
        Files.writeString(directory.resolve("orders.txt"), original);
        Files.delete(directory.resolve("customers.txt"));
        fails(() -> open(directory), IOException.class, "Missing user data is not replaced with demo users");
    }

    // Makes sure two application instances cannot overwrite the same data folder.
    private static void fileLock(Path directory) throws Exception {
        try (DeliverySystem first = open(directory)) {
            check(first.getCurrentUser() == null, "A newly opened system starts logged out");
            fails(() -> open(directory), IOException.class, "A second app cannot open the same data folder");
        }
        try (DeliverySystem reopened = open(directory)) {
            check(reopened.getCurrentUser() == null, "Closing the app releases its data-folder lock");
        }
    }

    // Checks values that could break prices, saved fields, and order sizes.
    private static void inputValidation() throws Exception {
        fails(() -> new MenuItem("Bad|name", BigDecimal.ONE), IllegalArgumentException.class, "Saved field separators are rejected");
        fails(() -> new MenuItem("Bad\nname\nagain", BigDecimal.ONE), IllegalArgumentException.class, "Multiline names cannot corrupt saved files");
        fails(() -> new MenuItem("Food", new BigDecimal("1.001")), IllegalArgumentException.class, "Fractions of cents are rejected");
        fails(() -> new MenuItem("Food", new BigDecimal("-1")), IllegalArgumentException.class, "Negative prices are rejected");
        fails(() -> new Order(1, "customer", List.of()), IllegalArgumentException.class, "Empty orders are rejected");
        fails(() -> User.normalizeUsername("bad|user"), IllegalArgumentException.class, "Invalid usernames are rejected");
    }

    // Opens a test system using the project's starting menu.
    private static DeliverySystem open(Path directory) throws IOException {
        return new DeliverySystem(directory, Path.of("Menu.txt"));
    }

    // Finds one order by number without assuming its position in a list.
    private static Order find(List<Order> orders, int id) {
        return orders.stream().filter(order -> order.getOrderId() == id).findFirst().orElseThrow();
    }

    // Checks one expected result and counts it for the test summary.
    private static void check(boolean result, String message) {
        if (!result) {
            throw new AssertionError(message);
        }
        checks++;
    }

    // Compares expected and actual values with a useful failure message.
    private static void equal(Object expected, Object actual, String message) {
        check(expected.equals(actual), message + ": expected " + expected + ", got " + actual);
    }

    // Checks that an invalid operation fails with the expected kind of error.
    private static void fails(CheckedAction action, Class<? extends Exception> type, String message) throws Exception {
        try {
            action.run();
        } catch (Exception ex) {
            check(type.isInstance(ex), message + ": unexpected " + ex);
            return;
        }
        throw new AssertionError(message + ": the operation succeeded unexpectedly");
    }
}
