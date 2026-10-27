import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class TerminalApp {
    private final DeliverySystem system;
    private final Scanner input = new Scanner(System.in);

    // Shares the same delivery system used by the desktop app.
    public TerminalApp(DeliverySystem system) {
        this.system = system;
    }

    // Runs the login menu and sends each user to their own dashboard.
    public void run() throws IOException {
        System.out.println("CS233 Delivery System");
        try {
            while (true) {
                try {
                    User user = system.getCurrentUser();
                    if (user == null) {
                        System.out.println("\n1. Log in\n2. Create customer account\n0. Exit");
                        String option = read("Choose: ");
                        if (option.equals("0")) {
                            break;
                        } else if (option.equals("1")) {
                            system.login(read("Username: "), read("Password: "));
                        } else if (option.equals("2")) {
                            addUser("Customer");
                        } else {
                            System.out.println("Choose one of the listed options.");
                        }
                    } else {
                        System.out.println("\n" + user.getRole() + ": " + user.getUsername());
                        user.viewOptions();
                        String option = read("Choose: ");
                        if (option.equals("0")) {
                            system.logout();
                        } else if (option.equals("9")) {
                            for (ActivityEntry entry : system.getActivity()) {
                                System.out.println(entry.getTime() + " | " + entry.getUsername() + " | "
                                        + entry.getEvent() + " | " + entry.getDetails());
                            }
                        } else if (user instanceof Admin) {
                            adminOption(option);
                        } else if (user instanceof Customer) {
                            customerOption(option);
                        } else {
                            driverOption(option);
                        }
                    }
                } catch (IllegalArgumentException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        } catch (NoSuchElementException ex) {
            // End of input also closes the program when commands are piped in.
        }
        System.out.println("Goodbye.");
    }

    // Handles the admin's menu and account options.
    private void adminOption(String option) throws IOException {
        switch (option) {
            case "1" -> showMenu();
            case "2" -> system.addMenuItem(read("Food name: "), new BigDecimal(read("Price: ")));
            case "3" -> system.updateMenu(read("Food name: "), new BigDecimal(read("New price: ")));
            case "4" -> system.removeMenuItem(read("Food name: "));
            case "5" -> showOrders(system.getAllOrders());
            case "6" -> addUser(read("Role (Admin, Customer, Driver): "));
            case "7" -> {
                for (Driver driver : system.getDrivers()) {
                    System.out.printf("%s | available: %s | rating: %.2f (%d ratings)%n",
                            driver.getUsername(), driver.getAvailability(), driver.getAverageRating(), driver.getRatings().size());
                }
            }
            case "8" -> changeProfile();
            default -> System.out.println("Choose one of the listed options.");
        }
    }

    // Handles ordering, order history, and customer ratings.
    private void customerOption(String option) throws IOException {
        switch (option) {
            case "1" -> showMenu();
            case "2" -> {
                showMenu();
                String[] names = read("Items, separated by commas (repeat an item for more than one): ").split(",", -1);
                ArrayList<String> items = new ArrayList<>();
                for (String name : names) {
                    items.add(name.trim());
                }
                Order order = system.placeOrder(items);
                System.out.println("Order #" + order.getOrderId() + " placed. Total: $" + order.calculateTotalCost());
            }
            case "3" -> showOrders(system.getMyOrders());
            case "4" -> {
                system.rateDriver(number("Order number: "), number("Rating (1-5): "));
                System.out.println("Rating saved.");
            }
            case "5" -> system.cancelOrder(number("Order number: "));
            case "6" -> changeProfile();
            default -> System.out.println("Choose one of the listed options.");
        }
    }

    // Handles the driver's assigned orders and delivery updates.
    private void driverOption(String option) throws IOException {
        switch (option) {
            case "1" -> showOrders(system.getMyOrders());
            case "2" -> system.startDelivery(number("Order number: "));
            case "3" -> system.completeDelivery(number("Order number: "));
            case "4" -> {
                Driver driver = (Driver) system.getCurrentUser();
                System.out.printf("Average: %.2f | Last ratings: %s%n", driver.getAverageRating(), driver.getRatings());
            }
            case "5" -> changeProfile();
            default -> System.out.println("Choose one of the listed options.");
        }
    }

    // Lists the food available for a new order.
    private void showMenu() {
        System.out.println("\nMenu");
        for (MenuItem item : system.getMenuItems()) {
            System.out.println(item.getName() + " - $" + item.getPrice());
        }
    }

    // Prints order details without exposing another customer's history.
    private void showOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            System.out.println("No orders yet.");
        }
        for (Order order : orders) {
            StringBuilder items = new StringBuilder();
            for (MenuItem item : order.getItems()) {
                if (!items.isEmpty()) {
                    items.append(", ");
                }
                items.append(item.getName());
            }
            System.out.printf("#%d | %s | %s | %s | $%s | driver: %s | rating: %s%n",
                    order.getOrderId(), order.getCustomer(), items, order.getStatus(), order.calculateTotalCost(),
                    order.getDriver().isEmpty() ? "waiting" : order.getDriver(),
                    order.getRating() == 0 ? "not rated" : order.getRating());
        }
    }

    // Collects the information for a new customer or an admin-created user.
    private void addUser(String role) throws IOException {
        system.register(role, read("Username: "), read("Password (6+ characters): "), read("Email: "));
        System.out.println("Account created.");
    }

    // Updates the current user's email and optional password.
    private void changeProfile() throws IOException {
        system.changeInfo(read("Email: "), read("New password (leave blank to keep): "));
        System.out.println("Profile saved.");
    }

    // Reads a full line so names and paths can contain spaces.
    private String read(String prompt) {
        System.out.print(prompt);
        return input.nextLine();
    }

    // Reads a whole number and gives a clear error for invalid input.
    private int number(String prompt) {
        try {
            return Integer.parseInt(read(prompt).trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a whole number.");
        }
    }
}
