import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class DiagramExport {
    private static final Color GREEN = new Color(27, 68, 58);
    private final int width;
    private final int height;
    private final BufferedImage image;
    private final Graphics2D graphics;
    private final StringBuilder svg = new StringBuilder();
    private final ArrayList<String> elements = new ArrayList<>();
    private int nextId;

    // Starts a diagram that can be exported without a browser or extra libraries.
    private DiagramExport(int width, int height, String title) {
        this.width = width;
        this.height = height;
        image = new BufferedImage(width * 2, height * 2, BufferedImage.TYPE_INT_RGB);
        graphics = image.createGraphics();
        graphics.scale(2, 2);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
                .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(width).append(' ').append(height)
                .append("\"><rect width=\"100%\" height=\"100%\" fill=\"white\"/>");
        text(50, 50, title, 28, true);
        text(50, 82, "Key fields and methods. + public   - private   # protected   ~ package access. Routine getters omitted.", 15, false);
    }

    // Creates the data-model diagram and the application-structure diagram.
    public static void main(String[] args) throws Exception {
        DiagramExport model = new DiagramExport(1420, 1360, "Delivery System - class model");
        model.box(480, 115, 440, "User (abstract)", new String[]{"- username: String", "- passwordHash: String", "- email: String"},
                new String[]{"+ login(String): boolean", "+ getRole(): String [abstract]", "+ viewOptions(): void [abstract]"});
        model.box(50, 440, 350, "Admin extends User", new String[]{},
                new String[]{"+ getRole(): String", "+ viewOptions(): void"});
        model.box(450, 440, 350, "Customer extends User", new String[]{},
                new String[]{"+ getRole(): String", "+ viewOptions(): void"});
        model.box(850, 440, 500, "Driver extends User", new String[]{"- availability: boolean", "- ratings: Deque<Integer>", "- averageRating: double"},
                new String[]{"~ addRating(int): void", "~ restoreRatings(Deque): void", "~ setAvailability(boolean): void",
                        "- calculateAvgRating(): void", "+ compareTo(Driver): int"});
        model.box(50, 850, 350, "Menu", new String[]{"- menu: HashMap<String,MenuItem>"},
                new String[]{"+ addMenuItem(MenuItem): void", "+ updateMenu(MenuItem): void", "+ removeMenuItem(String): void",
                        "+ getItem(String): MenuItem", "+ getItems(): ArrayList<MenuItem>"});
        model.box(450, 850, 500, "Order", new String[]{"- orderNum: int", "- customer: String [username]", "- items: ArrayList<MenuItem>",
                "- status: Status [enum]", "- driver: String [username]", "- rating: int [0 = not rated]"},
                new String[]{"+ calculateTotalCost(): BigDecimal", "+ canCancel(): boolean", "~ assignDriver(String): void", "~ startDelivery(): void",
                        "~ completeDelivery(): void", "~ cancel(): void", "~ rate(int): void"});
        model.box(1000, 1000, 350, "MenuItem (immutable)", new String[]{"- name: String", "- price: BigDecimal"},
                new String[]{"+ getName(): String", "+ getPrice(): BigDecimal"});
        model.edge(new int[][]{{225, 440}, {225, 390}, {700, 390}, {700, 343}}, true);
        model.edge(new int[][]{{625, 440}, {625, 390}, {700, 390}, {700, 343}}, true);
        model.edge(new int[][]{{1100, 440}, {1100, 390}, {700, 390}, {700, 343}}, true);
        model.edge(new int[][]{{625, 556}, {625, 760}, {700, 760}, {700, 850}}, false);
        model.text(640, 740, "places 0..* orders", 15, false);
        model.edge(new int[][]{{1100, 716}, {1100, 800}, {900, 800}, {900, 850}}, false);
        model.text(1010, 780, "assigned driver: 0..1", 15, false);
        model.edge(new int[][]{{225, 1078}, {225, 1280}, {1175, 1280}, {1175, 1180}}, false);
        model.text(650, 1260, "stores 0..* menu items", 15, false);
        model.edge(new int[][]{{950, 1050}, {1000, 1050}}, false);
        model.text(956, 1033, "1..*", 13, false);
        model.text(50, 1320, "Order.Status: PLACED -> ACCEPTED -> IN_PROGRESS -> DELIVERED. PLACED or ACCEPTED can become CANCELLED.", 16, false);
        model.save("docs/current-class-model");

        DiagramExport app = new DiagramExport(1600, 1650, "Delivery System - application structure");
        app.box(600, 115, 400, "Main", new String[]{}, new String[]{"+ main(String[]): void"});
        app.box(40, 300, 440, "DeliveryApp extends JFrame", new String[]{"- system: DeliverySystem", "- cart: LinkedHashMap<String,Integer>"},
                new String[]{"- showLogin(): void", "- showDashboard(String): void", "- activityPage(): JPanel", "- attempt(Action): void"});
        app.box(1120, 300, 440, "TerminalApp", new String[]{"- system: DeliverySystem", "- input: Scanner"},
                new String[]{"+ run(): void", "- adminOption(String): void", "- customerOption(String): void", "- driverOption(String): void"});
        app.box(550, 650, 500, "DeliverySystem", new String[]{"- files: FileManager", "- menu: Menu", "- users: LinkedHashMap<String,User>",
                "- allOrders: LinkedHashMap<Integer,Order>", "- ordersQueue: Queue<Order>", "- readyDrivers: PriorityQueue<Driver>", "- currentUser: User"},
                new String[]{"+ login(String,String): User", "+ register(...): void", "+ placeOrder(List<String>): Order",
                        "- giveOrderToDriver(): void", "+ startDelivery(int): void", "+ completeDelivery(int): void", "+ rateDriver(int,int): void",
                        "+ getActivity(): ArrayList<ActivityEntry>"});
        app.box(550, 1190, 500, "FileManager implements AutoCloseable", new String[]{"- directory: Path", "- lock: FileLock", "- actor: String", "- actorRole: String"},
                new String[]{"+ loadMenu(): Menu", "+ loadUsers(): LinkedHashMap", "+ loadOrders(users): LinkedHashMap", "+ saveUsers(users,role): void",
                        "+ appendOrder(order,ratings): void", "+ log(event,details): void", "+ loadActivity(User): ArrayList<ActivityEntry>"});
        app.box(40, 1210, 430, "PasswordUtil", new String[]{"- ITERATIONS: int [constant]"},
                new String[]{"+ hash(String): String", "+ matches(String,String): boolean", "+ isValidHash(String): boolean"});
        app.box(1120, 1210, 440, "ActivityEntry (immutable)", new String[]{"- time: String [UTC]", "- role: String", "- username: String", "- event: String", "- details: String"},
                new String[]{"+ getDetails(): String"});
        app.edge(new int[][]{{600, 160}, {260, 160}, {260, 300}}, false);
        app.text(320, 145, "opens desktop UI", 15, false);
        app.edge(new int[][]{{1000, 160}, {1340, 160}, {1340, 300}}, false);
        app.text(1110, 145, "or terminal with --cli", 15, false);
        app.edge(new int[][]{{800, 207}, {800, 650}}, false);
        app.text(815, 595, "creates shared system", 15, false);
        app.edge(new int[][]{{480, 425}, {520, 425}, {520, 760}, {550, 760}}, false);
        app.edge(new int[][]{{1120, 425}, {1080, 425}, {1080, 760}, {1050, 760}}, false);
        app.edge(new int[][]{{800, 1094}, {800, 1190}}, false);
        app.text(815, 1150, "loads and saves", 15, false);
        app.edge(new int[][]{{550, 1320}, {470, 1320}}, false);
        app.text(480, 1300, "hashes", 13, false);
        app.edge(new int[][]{{1050, 1320}, {1120, 1320}}, false);
        app.text(1060, 1300, "reads", 13, false);
        app.text(50, 1600, "Both interfaces use the same role checks, order rules, and text files. No separate server is needed.", 17, false);
        app.save("docs/current-app-structure");
    }

    // Draws a UML class with separate field and method sections.
    private void box(int x, int y, int w, String title, String[] fields, String[] methods) {
        if (graphics.getFontMetrics(new Font(Font.SANS_SERIF, Font.BOLD, 18)).stringWidth(title) > w - 32) {
            throw new IllegalArgumentException("Class title does not fit: " + title);
        }
        for (String[] section : new String[][]{fields, methods}) {
            for (String line : section) {
                if (graphics.getFontMetrics(new Font(Font.MONOSPACED, Font.PLAIN, 15)).stringWidth(line) > w - 32) {
                    throw new IllegalArgumentException("Class label does not fit: " + line);
                }
            }
        }
        int h = 68 + (fields.length + methods.length) * 24 + (fields.length == 0 ? 0 : 16);
        graphics.setColor(new Color(241, 246, 240));
        graphics.fillRect(x, y, w, 44);
        graphics.setColor(GREEN);
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.drawRect(x, y, w, h);
        graphics.drawLine(x, y + 44, x + w, y + 44);
        svg.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(w)
                .append("\" height=\"").append(h).append("\" fill=\"none\" stroke=\"#1b443a\"/>");
        svg.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(w)
                .append("\" height=\"44\" fill=\"#f1f6f0\" stroke=\"#1b443a\"/>");
        elements.add(shape("rectangle", x, y, w, h, ""));
        elements.add(shape("rectangle", x, y, w, 44, "").replace("\"backgroundColor\":\"transparent\"", "\"backgroundColor\":\"#f1f6f0\""));
        text(x + 16, y + 29, title, 18, true);
        int baseline = y + 70;
        for (String field : fields) {
            text(x + 16, baseline, field, 15, false);
            baseline += 24;
        }
        if (fields.length > 0) {
            int line = baseline - 8;
            graphics.drawLine(x, line, x + w, line);
            svg.append("<path d=\"M").append(x).append(' ').append(line).append("H").append(x + w)
                    .append("\" stroke=\"#1b443a\"/>");
            elements.add(shape("line", x, line, w, 0, ",\"points\":[[0,0],[" + w + ",0]],\"startBinding\":null,\"endBinding\":null"));
            baseline += 16;
        }
        for (String method : methods) {
            text(x + 16, baseline, method, 15, false);
            baseline += 24;
        }
    }

    // Draws text in the PNG and SVG and keeps it editable in Excalidraw.
    private void text(int x, int baseline, String value, int size, boolean bold) {
        graphics.setColor(GREEN);
        graphics.setFont(new Font(bold ? Font.SANS_SERIF : Font.MONOSPACED, bold ? Font.BOLD : Font.PLAIN, size));
        graphics.drawString(value, x, baseline);
        svg.append("<text x=\"").append(x).append("\" y=\"").append(baseline).append("\" font-family=\"")
                .append(bold ? "sans-serif" : "monospace").append("\" font-size=\"").append(size)
                .append("\" font-weight=\"").append(bold ? "bold" : "normal").append("\" fill=\"#1b443a\">")
                .append(value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")).append("</text>");
        int textWidth = graphics.getFontMetrics().stringWidth(value);
        elements.add(shape("text", x, baseline - size, textWidth, (int) (size * 1.25),
                ",\"text\":" + quote(value) + ",\"originalText\":" + quote(value) + ",\"fontSize\":" + size
                        + ",\"fontFamily\":" + (bold ? 2 : 3) + ",\"textAlign\":\"left\",\"verticalAlign\":\"top\",\"lineHeight\":1.25,\"containerId\":null,\"autoResize\":true"));
    }

    // Draws a relationship, using a hollow triangle for inheritance.
    private void edge(int[][] points, boolean inheritance) {
        Path2D path = new Path2D.Double();
        StringBuilder coordinates = new StringBuilder();
        StringBuilder relative = new StringBuilder("[");
        path.moveTo(points[0][0], points[0][1]);
        int maxX = points[0][0], minX = maxX, maxY = points[0][1], minY = maxY;
        for (int i = 0; i < points.length; i++) {
            int[] point = points[i];
            if (i > 0) {
                path.lineTo(point[0], point[1]);
                relative.append(',');
            }
            coordinates.append(point[0]).append(',').append(point[1]).append(' ');
            relative.append('[').append(point[0] - points[0][0]).append(',').append(point[1] - points[0][1]).append(']');
            maxX = Math.max(maxX, point[0]); minX = Math.min(minX, point[0]);
            maxY = Math.max(maxY, point[1]); minY = Math.min(minY, point[1]);
        }
        graphics.setColor(GREEN);
        graphics.setStroke(new BasicStroke(1.5f));
        graphics.draw(path);
        svg.append("<polyline points=\"").append(coordinates).append("\" fill=\"none\" stroke=\"#1b443a\"/>");
        int[] end = points[points.length - 1];
        int[] previous = points[points.length - 2];
        double angle = Math.atan2(end[1] - previous[1], end[0] - previous[0]);
        int ax = (int) (end[0] - 15 * Math.cos(angle - 0.5));
        int ay = (int) (end[1] - 15 * Math.sin(angle - 0.5));
        int bx = (int) (end[0] - 15 * Math.cos(angle + 0.5));
        int by = (int) (end[1] - 15 * Math.sin(angle + 0.5));
        java.awt.Polygon head = new java.awt.Polygon(new int[]{end[0], ax, bx}, new int[]{end[1], ay, by}, 3);
        graphics.setColor(inheritance ? Color.WHITE : GREEN);
        graphics.fillPolygon(head);
        graphics.setColor(GREEN);
        graphics.drawPolygon(head);
        svg.append("<polygon points=\"").append(end[0]).append(',').append(end[1]).append(' ').append(ax).append(',')
                .append(ay).append(' ').append(bx).append(',').append(by).append("\" fill=\"")
                .append(inheritance ? "white" : "#1b443a").append("\" stroke=\"#1b443a\"/>");
        elements.add(shape("arrow", points[0][0], points[0][1], maxX - minX, maxY - minY,
                ",\"points\":" + relative + "],\"startArrowhead\":null,\"endArrowhead\":\""
                        + (inheritance ? "triangle_outline" : "arrow") + "\",\"startBinding\":null,\"endBinding\":null"));
    }

    // Adds the standard fields used by editable Excalidraw elements.
    private String shape(String type, int x, int y, int w, int h, String extra) {
        return "{\"id\":\"e" + nextId++ + "\",\"type\":" + quote(type) + ",\"x\":" + x + ",\"y\":" + y
                + ",\"width\":" + w + ",\"height\":" + h + ",\"angle\":0,\"strokeColor\":\"#1b443a\",\"backgroundColor\":\"transparent\","
                + "\"fillStyle\":\"solid\",\"strokeWidth\":1.5,\"strokeStyle\":\"solid\",\"roughness\":0,\"opacity\":100,\"groupIds\":[],"
                + "\"roundness\":null,\"seed\":42,\"version\":1,\"versionNonce\":1,\"isDeleted\":false,\"boundElements\":null,\"updated\":0,\"link\":null,\"locked\":false"
                + extra + "}";
    }

    // Escapes diagram labels for JSON.
    private String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    // Saves the viewable images and a separate editable drawing.
    private void save(String base) throws Exception {
        Files.createDirectories(Path.of(base).getParent());
        graphics.dispose();
        ImageIO.write(image, "png", Path.of(base + ".png").toFile());
        Files.writeString(Path.of(base + ".svg"), svg + "</svg>\n");
        Files.writeString(Path.of(base + ".excalidraw"), "{\"type\":\"excalidraw\",\"version\":2,\"source\":\"https://excalidraw.com\",\"elements\":["
                + String.join(",", elements) + "],\"appState\":{\"viewBackgroundColor\":\"#ffffff\",\"gridSize\":null},\"files\":{}}\n");
        System.out.println("Exported " + base + " as PNG, SVG, and Excalidraw (" + width + " x " + height + ").");
    }
}
