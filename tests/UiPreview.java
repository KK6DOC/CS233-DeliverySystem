import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import javax.swing.SwingUtilities;

public class UiPreview {
    // Renders the actual role screens using temporary demo data and no visible window.
    public static void main(String[] args) throws Exception {
        Path output = args.length == 0 ? Path.of("build", "previews") : Path.of(args[0]);
        Files.createDirectories(output);
        Path temporary = Files.createTempDirectory("cs233-ui-preview-");
        Exception[] failure = new Exception[1];
        try (DeliverySystem system = new DeliverySystem(temporary, Path.of("Menu.txt"))) {
            SwingUtilities.invokeAndWait(() -> {
                DeliveryApp app = new DeliveryApp(system);
                try {
                    app.writePreview(output.resolve("login.png"), "");
                    system.login("customer", "customer123");
                    app.writePreview(output.resolve("customer.png"), "Menu");
                    system.placeOrder(List.of("Hamburger", "Fries", "Drink"));
                    app.writePreview(output.resolve("customer-orders.png"), "My orders");
                    system.logout();
                    system.login("driver1", "driver123");
                    system.startDelivery(1);
                    app.writePreview(output.resolve("driver.png"), "Deliveries");
                    system.completeDelivery(1);
                    system.logout();
                    system.login("customer", "customer123");
                    system.rateDriver(1, 5);
                    app.writePreview(output.resolve("customer-activity.png"), "Activity");
                    system.logout();
                    system.login("admin", "admin123");
                    app.writePreview(output.resolve("admin.png"), "All orders");
                    app.writePreview(output.resolve("admin-activity.png"), "Activity");
                } catch (Exception ex) {
                    failure[0] = ex;
                } finally {
                    app.dispose();
                }
            });
            if (failure[0] != null) {
                throw failure[0];
            }
            System.out.println("Rendered login, customer, driver, admin, and activity screens in " + output + ".");
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }
}
