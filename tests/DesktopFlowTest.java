import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class DesktopFlowTest {
    private static int checks;

    // Exercises real dashboard button handlers on the Swing thread without opening a window.
    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("cs233-desktop-test-");
        Throwable[] failure = new Throwable[1];
        try (DeliverySystem system = new DeliverySystem(temporary, Path.of("Menu.txt"))) {
            SwingUtilities.invokeAndWait(() -> {
                DeliveryApp app = new DeliveryApp(system);
                try {
                    runFlow(app, system);
                } catch (Throwable ex) {
                    failure[0] = ex;
                } finally {
                    app.dispose();
                }
            });
            if (failure[0] != null) {
                throw new AssertionError("Desktop flow failed", failure[0]);
            }
            System.out.println("Passed " + checks + " desktop checks without showing a window.");
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    // Checks customer checkout, driver updates, admin access, and role-specific activity screens.
    private static void runFlow(DeliveryApp app, DeliverySystem system) {
        JComboBox<?> demo = null;
        for (Component component : components(app.getContentPane())) {
            if (component instanceof JComboBox<?> box) {
                demo = box;
            }
        }
        check(demo != null, "The demo account picker exists");
        demo.setSelectedIndex(1);
        check(field(app, "username").getText().equals("customer"), "Demo selection fills in the username");
        check(new String(((JPasswordField) field(app, "password")).getPassword()).equals("customer123"),
                "Demo selection fills in the password");
        button(app, "Sign in").doClick();
        check(system.getCurrentUser() instanceof Customer, "Sign-in opens the customer dashboard");
        check(!button(app, "Place order").isEnabled(), "Checkout is disabled for an empty cart");
        button(app, "+ Add to order").doClick();
        check(button(app, "Place order").isEnabled(), "Adding food enables checkout");
        button(app, "Place order").doClick();
        check(system.getMyOrders().size() == 1, "The checkout button creates an order");
        check(table(app).getRowCount() == 1, "Checkout opens the customer's order history");
        check(table(app).getColumnCount() == 6, "Customer history leaves out the redundant customer column");
        check(!button(app, "Cancel order").isEnabled(), "Cancellation needs a selected order");
        table(app).setRowSelectionInterval(0, 0);
        check(button(app, "Cancel order").isEnabled(), "An assigned order enables cancellation before delivery starts");
        table(app).clearSelection();
        check(!button(app, "Cancel order").isEnabled(), "Clearing the order selection disables cancellation");
        button(app, "Activity").doClick();
        check(table(app).getRowCount() >= 5, "The customer activity page shows real request and save events");
        button(app, "Log out").doClick();
        check(system.getCurrentUser() == null, "Log out clears the session");
        signIn(app, "driver1", "driver123");
        check(system.getCurrentUser() instanceof Driver, "Drivers get a different dashboard");
        check(!button(app, "Start delivery").isEnabled(), "Delivery actions need a selected order");
        table(app).setRowSelectionInterval(0, 0);
        check(button(app, "Start delivery").isEnabled(), "Accepted deliveries can be started");
        button(app, "Start delivery").doClick();
        check(system.getMyOrders().get(0).getStatus() == Order.Status.IN_PROGRESS, "Start delivery updates the backend");
        table(app).setRowSelectionInterval(0, 0);
        check(!button(app, "Start delivery").isEnabled(), "An in-progress order cannot be started again");
        check(button(app, "Mark delivered").isEnabled(), "An in-progress delivery can be completed");
        button(app, "Mark delivered").doClick();
        check(system.getMyOrders().get(0).getStatus() == Order.Status.DELIVERED, "Mark delivered saves the completed state");
        button(app, "My ratings").doClick();
        button(app, "Activity").doClick();
        check(table(app).getRowCount() > 0, "Drivers have their own activity page");
        button(app, "Log out").doClick();
        signIn(app, "customer", "customer123");
        button(app, "My orders").doClick();
        table(app).setRowSelectionInterval(0, 0);
        check(button(app, "Rate delivery").isEnabled(), "A delivered order enables the rating action");
        check(!button(app, "Cancel order").isEnabled(), "Delivered orders cannot be cancelled");
        button(app, "Log out").doClick();
        signIn(app, "admin", "admin123");
        check(system.getCurrentUser() instanceof Admin, "Admins get the all-orders dashboard");
        check(table(app).getRowCount() == 1, "The admin can see the delivered order");
        button(app, "Menu").doClick();
        check(button(app, "Add item").isEnabled(), "Admins can reach menu editing");
        button(app, "Drivers").doClick();
        check(table(app).getRowCount() == 2, "The admin can see the driver pool");
        button(app, "Accounts").doClick();
        check(button(app, "Create an account").isEnabled(), "Admins can reach role-based account creation");
        button(app, "Activity").doClick();
        check(table(app).getRowCount() > 20, "Admin activity includes all roles and startup events");
    }

    // Signs in through the actual login button handler.
    private static void signIn(DeliveryApp app, String username, String password) {
        field(app, "username").setText(username);
        field(app, "password").setText(password);
        button(app, "Sign in").doClick();
    }

    // Finds a named text field in the currently displayed screen.
    private static JTextField field(DeliveryApp app, String name) {
        for (Component component : components(app.getContentPane())) {
            if (component instanceof JTextField field && name.equals(field.getName())) {
                return field;
            }
        }
        throw new AssertionError("Missing field: " + name);
    }

    // Finds a real button by its visible label.
    private static JButton button(DeliveryApp app, String text) {
        for (Component component : components(app.getContentPane())) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
        }
        throw new AssertionError("Missing button: " + text);
    }

    // Finds the table belonging to the current dashboard page.
    private static JTable table(DeliveryApp app) {
        for (Component component : components(app.getContentPane())) {
            if (component instanceof JTable table) {
                return table;
            }
        }
        throw new AssertionError("Missing dashboard table");
    }

    // Walks nested panels without relying on private fields or reflection.
    private static ArrayList<Component> components(Container root) {
        ArrayList<Component> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            result.add(child);
            if (child instanceof Container nested) {
                result.addAll(components(nested));
            }
        }
        return result;
    }

    // Counts verified UI behavior and reports a useful failure message.
    private static void check(boolean result, String message) {
        if (!result) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
