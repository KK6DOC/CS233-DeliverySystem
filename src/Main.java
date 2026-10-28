import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {
    // Starts the desktop app by default, or the terminal version with --cli.
    public static void main(String[] args) {
        boolean cli = false;
        Path directory = Path.of("data");
        try {
            Path startingMenu = Path.of("Menu.txt");
            if (!java.nio.file.Files.exists(startingMenu)) {
                Path jar = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                startingMenu = jar.getParent().resolve("Menu.txt");
                directory = Path.of(System.getProperty("user.home"), ".cs233-delivery");
            }
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--cli" -> cli = true;
                    case "--data-dir" -> {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Give a folder after --data-dir.");
                        }
                        directory = Path.of(args[i]);
                    }
                    case "--help" -> {
                        System.out.println("Usage: java -cp build/classes Main [--cli] [--data-dir folder]");
                        return;
                    }
                    default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
                }
            }
            if (!cli && java.awt.GraphicsEnvironment.isHeadless()) {
                throw new IllegalArgumentException("A desktop display is unavailable. Start with --cli for terminal mode.");
            }
            DeliverySystem system = new DeliverySystem(directory, startingMenu);
            if (cli) {
                try (system) {
                    new TerminalApp(system).run();
                }
            } else {
                SwingUtilities.invokeLater(() -> new DeliveryApp(system).setVisible(true));
            }
        } catch (IOException | IllegalArgumentException | java.net.URISyntaxException ex) {
            System.err.println("Could not start or save the app: " + ex.getMessage());
            if (!cli && !java.awt.GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Delivery System", JOptionPane.ERROR_MESSAGE);
            }
            System.exit(1);
        }
    }
}
