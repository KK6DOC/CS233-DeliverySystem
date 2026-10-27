import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TerminalFlowTest {
    private static int checks;

    // Runs complete terminal sessions in separate Java processes, including a packaged JAR.
    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("cs233 terminal tests ");
        try {
            Path data = temporary.resolve("saved data");
            List<String> launch = List.of("-cp", Path.of(System.getProperty("java.class.path")).toAbsolutePath().toString(), "Main");
            String commands = String.join("\n", "1", "customer", "customer123", "2", "Hamburger,Fries,Drink", "0",
                    "1", "driver1", "driver123", "2", "1", "3", "1", "0", "1", "customer", "customer123",
                    "4", "1", "2.5", "4", "1", "5", "3", "9", "0", "0", "");
            String output = session(launch, data, Path.of("."), commands, temporary.resolve("first.txt"));
            check(output.contains("Order #1 placed. Total: $15.47"), "Terminal checkout saves the correct total");
            check(output.contains("Enter a whole number."), "Invalid rating input returns to the menu");
            check(output.contains("DELIVERED") && output.contains("rating: 5"), "Terminal delivery and rating actions complete the order");
            check(output.contains("Appended order #1 to orders.txt"), "The terminal Activity option displays actual saves");
            String restart = session(launch, data, Path.of("."), "1\ncustomer\ncustomer123\n3\n0\n0\n",
                    temporary.resolve("restart.txt"));
            check(restart.contains("DELIVERED") && restart.contains("rating: 5"), "A fresh process restores the completed order and rating");

            Path packaged = Files.createDirectories(temporary.resolve("packaged app"));
            Path jar = packaged.resolve("CS233DeliverySystem.jar");
            int result = java.util.spi.ToolProvider.findFirst("jar").orElseThrow().run(System.out, System.err,
                    "--create", "--file", jar.toString(), "--main-class", "Main", "-C", launch.get(1), ".");
            check(result == 0, "The application can be packaged as a JAR");
            Files.copy(Path.of("Menu.txt"), packaged.resolve("Menu.txt"));
            Path elsewhere = Files.createDirectories(temporary.resolve("another working directory"));
            String jarOutput = session(List.of("-jar", jar.toString()), temporary.resolve("jar data"), elsewhere,
                    "1\ncustomer\ncustomer123\n2\nHamburger,Fries,Drink\n0\n0\n", temporary.resolve("jar.txt"));
            check(jarOutput.contains("Order #1 placed. Total: $15.47"), "The JAR finds its starting menu outside the project folder");
            System.out.println("Passed " + checks + " terminal and packaged-JAR checks.");
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    // Starts a child process with paths passed as separate arguments so spaces work on Windows too.
    private static String session(List<String> launch, Path data, Path working, String input, Path output) throws Exception {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        ArrayList<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString());
        command.addAll(launch);
        command.addAll(List.of("--cli", "--data-dir", data.toAbsolutePath().toString()));
        Process process = new ProcessBuilder(command).directory(working.toAbsolutePath().toFile())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try (var stdin = process.getOutputStream()) {
            stdin.write(input.getBytes(StandardCharsets.UTF_8));
        }
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("The terminal session did not finish.");
        }
        String text = Files.readString(output);
        check(process.exitValue() == 0, "Terminal process exits normally: " + text);
        return text;
    }

    // Counts verified results and reports any failed session.
    private static void check(boolean result, String message) {
        if (!result) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
