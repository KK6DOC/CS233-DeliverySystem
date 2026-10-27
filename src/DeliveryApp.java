import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

public final class DeliveryApp extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Color GREEN = new Color(27, 68, 58);
    private static final Color BACKGROUND = new Color(245, 246, 241);
    private static final Color TEXT = new Color(31, 48, 42);
    private static final Color MUTED = new Color(103, 117, 109);
    private static final Color LINE = new Color(222, 229, 220);
    private static final Color ORANGE = new Color(227, 137, 87);
    private final transient DeliverySystem system;
    private final LinkedHashMap<String, Integer> cart = new LinkedHashMap<>();
    private String notice = "";

    @FunctionalInterface
    private interface Action {
        // Lets screen actions report file errors to the shared error handler.
        void run() throws IOException, java.text.ParseException;
    }

    // Sets up the desktop window and opens the login screen.
    public DeliveryApp(DeliverySystem system) {
        super("CS233 Delivery System");
        this.system = system;
        UIManager.put("OptionPane.messageFont", font(14, false));
        UIManager.put("OptionPane.buttonFont", font(14, false));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(960, 660));
        setSize(1080, 720);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            // Releases the saved-data folder when the window closes.
            @Override
            public void windowClosing(WindowEvent event) {
                try {
                    system.close();
                } catch (IOException ex) {
                    System.err.println("Could not release the data folder: " + ex.getMessage());
                }
            }
        });
        showLogin();
    }

    // Shows the sign-in form, demo accounts, and customer sign-up button.
    private void showLogin() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        JPanel intro = vertical(GREEN, 48);
        intro.setPreferredSize(new Dimension(360, 0));
        intro.add(label("CS 233  /  GROUP PROJECT", 12, false, new Color(179, 208, 186)));
        intro.add(Box.createVerticalGlue());
        intro.add(label("Delivery", 46, true, Color.WHITE));
        intro.add(label("System", 46, true, Color.WHITE));
        intro.add(Box.createVerticalStrut(24));
        intro.add(label("Your next meal starts here.", 18, false, Color.WHITE));
        intro.add(Box.createVerticalStrut(12));
        intro.add(label("Order food. Follow your delivery.", 14, false, new Color(192, 214, 198)));
        intro.add(label("Leave a rating when it arrives.", 14, false, new Color(192, 214, 198)));
        intro.add(Box.createVerticalGlue());
        intro.add(label("NOBODY LOVES JAVA", 12, true, new Color(179, 208, 186)));
        root.add(intro, BorderLayout.WEST);

        JPanel space = new JPanel(new java.awt.GridBagLayout());
        space.setBackground(BACKGROUND);
        JPanel form = card(32);
        form.setPreferredSize(new Dimension(400, 510));
        form.add(label("Welcome back", 30, true, TEXT));
        form.add(Box.createVerticalStrut(8));
        form.add(label("Sign in to your account to get started.", 14, false, MUTED));
        form.add(Box.createVerticalStrut(28));
        JTextField username = field();
        username.setName("username");
        JPasswordField password = new JPasswordField();
        styleField(password);
        password.setName("password");
        addField(form, "Username", username);
        addField(form, "Password", password);
        JButton signIn = button("Sign in", GREEN, Color.WHITE);
        signIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        signIn.addActionListener(event -> attempt(() -> {
            system.login(username.getText(), new String(password.getPassword()));
            notice = "";
            showDashboard(defaultSection());
        }));
        form.add(signIn);
        form.add(Box.createVerticalStrut(12));
        JButton signUp = button("Create a customer account", Color.WHITE, GREEN);
        signUp.setAlignmentX(Component.LEFT_ALIGNMENT);
        signUp.addActionListener(event -> attempt(() -> createAccount(false)));
        form.add(signUp);
        form.add(Box.createVerticalStrut(24));
        form.add(label("Trying the project? Pick a demo account.", 12, false, MUTED));
        form.add(Box.createVerticalStrut(8));
        JComboBox<String> demo = new JComboBox<>(new String[]{"Choose an account", "Customer", "Driver 1", "Driver 2", "Admin"});
        demo.setFont(font(13, false));
        demo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        demo.setAlignmentX(Component.LEFT_ALIGNMENT);
        demo.addActionListener(event -> {
            int selected = demo.getSelectedIndex();
            if (selected > 0) {
                String[] names = {"", "customer", "driver1", "driver2", "admin"};
                String[] passwords = {"", "customer123", "driver123", "driver123", "admin123"};
                username.setText(names[selected]);
                password.setText(passwords[selected]);
            }
        });
        form.add(demo);
        space.add(form);
        root.add(space, BorderLayout.CENTER);
        getRootPane().setDefaultButton(signIn);
        replaceContent(root);
    }

    // Opens the first useful page for the current user's role.
    private String defaultSection() {
        if (system.getCurrentUser() instanceof Customer) {
            return "Menu";
        }
        return system.getCurrentUser() instanceof Driver ? "Deliveries" : "All orders";
    }

    // Builds a sidebar and the selected role's page.
    private void showDashboard(String selected) {
        User user = system.getCurrentUser();
        JPanel root = new JPanel(new BorderLayout());
        JPanel sidebar = vertical(GREEN, 24);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.add(label("DELIVERY", 25, true, Color.WHITE));
        sidebar.add(label("SYSTEM", 25, true, Color.WHITE));
        sidebar.add(Box.createVerticalStrut(12));
        sidebar.add(label(user.getRole() + " dashboard", 13, false, new Color(190, 212, 197)));
        sidebar.add(Box.createVerticalStrut(40));
        String[] sections = user instanceof Admin ? new String[]{"All orders", "Menu", "Drivers", "Accounts", "Activity"}
                : user instanceof Customer ? new String[]{"Menu", "My orders", "Activity"}
                : new String[]{"Deliveries", "My ratings", "Activity"};
        for (String section : sections) {
            JButton navigation = button(section, selected.equals(section) ? new Color(62, 104, 86) : GREEN, Color.WHITE);
            navigation.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            navigation.setAlignmentX(Component.LEFT_ALIGNMENT);
            navigation.addActionListener(event -> showDashboard(section));
            sidebar.add(navigation);
            sidebar.add(Box.createVerticalStrut(10));
        }
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(label(user.getUsername(), 17, true, Color.WHITE));
        sidebar.add(Box.createVerticalStrut(10));
        JButton profile = button("My profile", GREEN, new Color(195, 218, 202));
        profile.setAlignmentX(Component.LEFT_ALIGNMENT);
        profile.addActionListener(event -> attempt(() -> changeProfile(selected)));
        sidebar.add(profile);
        JButton logout = button("Log out", GREEN, Color.WHITE);
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.addActionListener(event -> attempt(() -> {
            system.logout();
            cart.clear();
            notice = "";
            showLogin();
        }));
        sidebar.add(logout);
        root.add(sidebar, BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout(0, 22));
        main.setBackground(BACKGROUND);
        main.setBorder(BorderFactory.createEmptyBorder(30, 28, 26, 28));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel heading = vertical(BACKGROUND, 0);
        heading.add(label(selected.equals("Menu") && user instanceof Customer ? "What's on the menu?" : selected, 29, true, TEXT));
        heading.add(Box.createVerticalStrut(7));
        heading.add(label(subtitle(selected), 13, false, MUTED));
        if (!notice.isEmpty()) {
            heading.add(Box.createVerticalStrut(10));
            heading.add(label(notice, 13, true, GREEN));
        }
        header.add(heading, BorderLayout.CENTER);
        JButton refresh = button("Refresh", Color.WHITE, GREEN);
        refresh.addActionListener(event -> showDashboard(selected));
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        tools.setOpaque(false);
        tools.add(refresh);
        header.add(tools, BorderLayout.EAST);
        JPanel top = new JPanel(new BorderLayout(0, 22));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        if (selected.equals("All orders") || selected.equals("My orders") || selected.equals("Deliveries")) {
            top.add(orderSummary(user instanceof Admin ? system.getAllOrders() : system.getMyOrders()), BorderLayout.SOUTH);
        }
        main.add(top, BorderLayout.NORTH);
        JPanel content;
        if (selected.equals("Activity")) {
            content = activityPage();
        } else if (selected.equals("Menu")) {
            content = user instanceof Admin ? adminMenu() : customerMenu();
        } else if (selected.equals("Drivers")) {
            content = driversPage();
        } else if (selected.equals("Accounts")) {
            content = accountsPage();
        } else if (selected.equals("My ratings")) {
            content = ratingsPage();
        } else {
            content = ordersPage(selected);
        }
        main.add(content, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
        getRootPane().setDefaultButton(null);
        replaceContent(root);
    }

    // Gives each page a short explanation of what the user can do there.
    private String subtitle(String section) {
        return switch (section) {
            case "Menu" -> system.getCurrentUser() instanceof Admin
                    ? "Add food, change prices, or take an item off the menu." : "Pick your food and add it to your order.";
            case "My orders" -> "Follow your orders and rate completed deliveries.";
            case "Deliveries" -> "Start an assigned order, then mark it delivered when it arrives.";
            case "Drivers" -> "See who's available and how customers rated their deliveries.";
            case "Accounts" -> "Add a driver, customer, or another admin.";
            case "My ratings" -> "Your average uses your ten most recent customer ratings.";
            case "Activity" -> "Actual requests and file saves. Select a row to see its full details.";
            default -> "Orders are assigned as drivers become available.";
        };
    }

    // Shows food cards next to the customer's cart.
    private JPanel customerMenu() {
        JPanel page = new JPanel(new BorderLayout(18, 0));
        page.setOpaque(false);
        JPanel cards = new JPanel(new GridLayout(0, 2, 14, 14));
        cards.setBackground(BACKGROUND);
        for (MenuItem item : system.getMenuItems()) {
            JPanel card = card(20);
            card.setBorder(BorderFactory.createEmptyBorder(22, 20, 22, 20));
            JLabel initial = label(item.getName().substring(0, 1).toUpperCase(java.util.Locale.ROOT), 36, true, GREEN);
            card.add(initial);
            card.add(Box.createVerticalStrut(14));
            card.add(label(item.getName(), 20, true, TEXT));
            card.add(Box.createVerticalStrut(8));
            card.add(label("$" + item.getPrice(), 20, false, GREEN));
            card.add(Box.createVerticalStrut(18));
            JButton add = button("+ Add to order", GREEN, Color.WHITE);
            add.setAlignmentX(Component.LEFT_ALIGNMENT);
            add.addActionListener(event -> attempt(() -> {
                if (cartSize() == 100) {
                    throw new IllegalArgumentException("An order can have up to 100 items.");
                }
                cart.merge(item.getName(), 1, Integer::sum);
                notice = "";
                showDashboard("Menu");
            }));
            card.add(add);
            cards.add(card);
        }
        JPanel aligned = new JPanel(new BorderLayout());
        aligned.setOpaque(false);
        if (system.getMenuItems().isEmpty()) {
            aligned.add(label("The menu is empty. Check back soon.", 16, false, MUTED), BorderLayout.NORTH);
        } else {
            aligned.add(cards, BorderLayout.NORTH);
        }
        page.add(scroll(aligned), BorderLayout.CENTER);
        page.add(cartPanel(), BorderLayout.EAST);
        return page;
    }

    // Shows quantities, the total, and the checkout controls.
    private JPanel cartPanel() {
        JPanel card = new CardPanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(22, 18, 22, 18));
        card.setPreferredSize(new Dimension(255, 0));
        JPanel heading = vertical(Color.WHITE, 0);
        heading.add(label("Your order", 23, true, TEXT));
        heading.add(Box.createVerticalStrut(6));
        heading.add(label(cartSize() + " items", 13, false, MUTED));
        card.add(heading, BorderLayout.NORTH);
        String[][] rows = new String[cart.size()][3];
        BigDecimal total = new BigDecimal("0.00");
        ArrayList<String> names = new ArrayList<>(cart.keySet());
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            BigDecimal price = menuPrice(name).multiply(BigDecimal.valueOf(cart.get(name)));
            total = total.add(price);
            rows[i] = new String[]{name, String.valueOf(cart.get(name)), "$" + price};
        }
        JTable table = table(rows, new String[]{"Item", "Qty", "Price"});
        if (cart.isEmpty()) {
            JPanel empty = new JPanel(new java.awt.GridBagLayout());
            empty.setBackground(Color.WHITE);
            JPanel message = vertical(Color.WHITE, 0);
            message.add(label("Your cart is empty", 16, true, MUTED));
            message.add(Box.createVerticalStrut(8));
            message.add(label("Add food from the menu.", 12, false, MUTED));
            empty.add(message);
            card.add(empty, BorderLayout.CENTER);
        } else {
            card.add(scroll(table), BorderLayout.CENTER);
        }
        JPanel actions = vertical(Color.WHITE, 0);
        JButton remove = button("Remove one", BACKGROUND, GREEN);
        remove.setAlignmentX(Component.LEFT_ALIGNMENT);
        remove.setEnabled(!cart.isEmpty());
        remove.addActionListener(event -> attempt(() -> {
            int row = selectedRow(table);
            String name = names.get(row);
            if (cart.get(name) == 1) {
                cart.remove(name);
            } else {
                cart.put(name, cart.get(name) - 1);
            }
            showDashboard("Menu");
        }));
        actions.add(remove);
        actions.add(Box.createVerticalStrut(20));
        actions.add(label("Total  $" + total, 23, true, TEXT));
        actions.add(Box.createVerticalStrut(18));
        JButton place = button("Place order", ORANGE, TEXT);
        place.setAlignmentX(Component.LEFT_ALIGNMENT);
        place.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        place.setEnabled(!cart.isEmpty());
        place.addActionListener(event -> attempt(() -> {
            ArrayList<String> items = new ArrayList<>();
            for (String name : cart.keySet()) {
                for (int i = 0; i < cart.get(name); i++) {
                    items.add(name);
                }
            }
            Order order = system.placeOrder(items);
            cart.clear();
            notice = "Order #" + order.getOrderId() + " placed. Total: $" + order.calculateTotalCost();
            showDashboard("My orders");
        }));
        actions.add(place);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    // Counts quantities rather than just different item names.
    private int cartSize() {
        int total = 0;
        for (int count : cart.values()) {
            total += count;
        }
        return total;
    }

    // Looks up a food's current menu price for the cart preview.
    private BigDecimal menuPrice(String name) {
        for (MenuItem item : system.getMenuItems()) {
            if (item.getName().equals(name)) {
                return item.getPrice();
            }
        }
        throw new IllegalArgumentException("That item is no longer available.");
    }

    // Gives admins a menu table and buttons to add, update, and remove food.
    private JPanel adminMenu() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);
        ArrayList<MenuItem> items = system.getMenuItems();
        String[][] rows = new String[items.size()][2];
        for (int i = 0; i < items.size(); i++) {
            rows[i] = new String[]{items.get(i).getName(), "$" + items.get(i).getPrice()};
        }
        JTable table = table(rows, new String[]{"Food", "Price"});
        page.add(scroll(table), BorderLayout.CENTER);
        JPanel actions = actions();
        JButton add = button("Add item", GREEN, Color.WHITE);
        add.addActionListener(event -> attempt(() -> editMenu(null)));
        JButton update = button("Change price", Color.WHITE, GREEN);
        update.setEnabled(!items.isEmpty());
        update.addActionListener(event -> attempt(() -> editMenu(items.get(selectedRow(table)))));
        JButton remove = button("Remove item", Color.WHITE, GREEN);
        remove.setEnabled(!items.isEmpty());
        remove.addActionListener(event -> attempt(() -> {
            MenuItem item = items.get(selectedRow(table));
            if (JOptionPane.showConfirmDialog(this, "Remove " + item.getName() + " from the menu?",
                    "Remove item", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                system.removeMenuItem(item.getName());
                notice = "Menu item removed.";
                showDashboard("Menu");
            }
        }));
        actions.add(add);
        actions.add(update);
        actions.add(remove);
        page.add(actions, BorderLayout.SOUTH);
        return page;
    }

    // Asks for an item's name and price before saving the menu.
    private void editMenu(MenuItem existing) throws IOException {
        JPanel form = vertical(Color.WHITE, 12);
        JTextField name = field();
        JTextField price = field();
        if (existing != null) {
            name.setText(existing.getName());
            name.setEditable(false);
            price.setText(existing.getPrice().toString());
        }
        addField(form, "Food name", name);
        addField(form, "Price", price);
        if (JOptionPane.showConfirmDialog(this, form, existing == null ? "Add item" : "Change price",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            BigDecimal amount;
            try {
                amount = new BigDecimal(price.getText().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Enter a price such as 8.99.");
            }
            if (existing == null) {
                system.addMenuItem(name.getText(), amount);
            } else {
                system.updateMenu(name.getText(), amount);
            }
            notice = "Menu saved.";
            showDashboard("Menu");
        }
    }

    // Shows order history with actions limited to the logged-in user's role.
    private JPanel ordersPage(String section) {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);
        boolean admin = system.getCurrentUser() instanceof Admin;
        boolean driver = system.getCurrentUser() instanceof Driver;
        ArrayList<Order> orders = admin ? system.getAllOrders() : system.getMyOrders();
        String[][] rows = new String[orders.size()][7];
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            rows[i] = new String[]{"#" + order.getOrderId(), order.getCustomer(), itemNames(order.getItems()),
                    order.getStatus().toString().toLowerCase(java.util.Locale.ROOT).replace('_', ' '),
                    "$" + order.calculateTotalCost(), order.getDriver().isEmpty() ? "Waiting" : order.getDriver(),
                    order.getRating() == 0 ? "-" : order.getRating() + "/5"};
        }
        JTable table = table(rows, new String[]{"Order", "Customer", "Items", "Status", "Total", "Driver", "Rating"});
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            // Gives delivery status a readable color without relying on color alone.
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
                    boolean focused, int row, int column) {
                Component cell = super.getTableCellRendererComponent(source, value, selected, focused, row, column);
                if (!selected) {
                    String status = String.valueOf(value);
                    cell.setBackground(status.equals("delivered") ? new Color(231, 242, 230)
                            : status.equals("cancelled") ? new Color(247, 233, 227) : Color.WHITE);
                    cell.setForeground(status.equals("in progress") ? new Color(173, 94, 36) : GREEN);
                }
                return cell;
            }
        });
        if (!admin) {
            table.removeColumn(table.getColumnModel().getColumn(driver ? 5 : 1));
        }
        page.add(scroll(table), BorderLayout.CENTER);
        JPanel bottom = vertical(BACKGROUND, 0);
        if (orders.isEmpty()) {
            bottom.add(label(driver ? "No deliveries assigned yet." : "No orders yet.", 15, false, MUTED));
        } else if (!admin) {
            JPanel actions = actions();
            JButton first = button(driver ? "Start delivery" : "Rate delivery", GREEN, Color.WHITE);
            JButton second = button(driver ? "Mark delivered" : "Cancel waiting order", Color.WHITE, GREEN);
            first.setEnabled(false);
            second.setEnabled(false);
            table.getSelectionModel().addListSelectionListener(event -> {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    Order order = orders.get(table.convertRowIndexToModel(row));
                    first.setEnabled(driver ? order.getStatus() == Order.Status.ACCEPTED
                            : order.getStatus() == Order.Status.DELIVERED && order.getRating() == 0);
                    second.setEnabled(driver ? order.getStatus() == Order.Status.IN_PROGRESS
                            : order.getStatus() == Order.Status.PLACED);
                }
            });
            first.addActionListener(event -> attempt(() -> {
                Order order = orders.get(selectedRow(table));
                if (driver) {
                    system.startDelivery(order.getOrderId());
                    notice = "Order #" + order.getOrderId() + " is on its way.";
                } else {
                    JSpinner rating = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
                    JPanel form = vertical(Color.WHITE, 12);
                    addField(form, "Rate " + order.getDriver() + " from 1 to 5", rating);
                    if (JOptionPane.showConfirmDialog(this, form, "Rate delivery", JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    String value = ((JSpinner.DefaultEditor) rating.getEditor()).getTextField().getText().trim();
                    if (!value.matches("[1-5]")) {
                        throw new IllegalArgumentException("Enter a whole number from 1 to 5.");
                    }
                    system.rateDriver(order.getOrderId(), Integer.parseInt(value));
                    notice = "Thanks! Your rating was saved.";
                }
                showDashboard(section);
            }));
            second.addActionListener(event -> attempt(() -> {
                Order order = orders.get(selectedRow(table));
                if (driver) {
                    system.completeDelivery(order.getOrderId());
                    notice = "Order #" + order.getOrderId() + " delivered.";
                } else {
                    if (JOptionPane.showConfirmDialog(this, "Cancel order #" + order.getOrderId() + "?",
                            "Cancel order", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                        return;
                    }
                    system.cancelOrder(order.getOrderId());
                    notice = "Order cancelled.";
                }
                showDashboard(section);
            }));
            actions.add(first);
            actions.add(second);
            bottom.add(actions);
            bottom.add(Box.createVerticalStrut(10));
            bottom.add(label("Select an order to see its available actions.", 12, false, MUTED));
        }
        JButton details = button("View order details", Color.WHITE, GREEN);
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.setEnabled(!orders.isEmpty());
        details.addActionListener(event -> attempt(() -> {
            Order order = orders.get(selectedRow(table));
            javax.swing.JTextArea text = new javax.swing.JTextArea("Order #" + order.getOrderId()
                    + "\nCustomer: " + order.getCustomer() + "\nItems: " + itemNames(order.getItems())
                    + "\nTotal: $" + order.calculateTotalCost() + "\nStatus: " + order.getStatus()
                    + "\nDriver: " + (order.getDriver().isEmpty() ? "Waiting for a driver" : order.getDriver())
                    + "\nRating: " + (order.getRating() == 0 ? "Not rated" : order.getRating() + "/5"), 8, 38);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setFont(font(14, false));
            JOptionPane.showMessageDialog(this, scroll(text), "Order details", JOptionPane.PLAIN_MESSAGE);
        }));
        bottom.add(Box.createVerticalStrut(12));
        bottom.add(details);
        page.add(bottom, BorderLayout.SOUTH);
        return page;
    }

    // Shows the useful order counts above each role's order table.
    private JPanel orderSummary(List<Order> orders) {
        int active = 0;
        int delivered = 0;
        for (Order order : orders) {
            if (order.getStatus() == Order.Status.DELIVERED) {
                delivered++;
            } else if (order.getStatus() != Order.Status.CANCELLED) {
                active++;
            }
        }
        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 0));
        cards.setOpaque(false);
        String[] labels = {"Total orders", "Active", "Delivered"};
        int[] values = {orders.size(), active, delivered};
        for (int i = 0; i < labels.length; i++) {
            JPanel card = card(18);
            card.add(label(labels[i], 12, false, MUTED));
            card.add(Box.createVerticalStrut(8));
            card.add(label(String.valueOf(values[i]), 27, true, GREEN));
            cards.add(card);
        }
        return cards;
    }

    // Shows each driver's availability and recent average rating.
    private JPanel driversPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setOpaque(false);
        ArrayList<Driver> drivers = system.getDrivers();
        String[][] rows = new String[drivers.size()][4];
        for (int i = 0; i < drivers.size(); i++) {
            Driver driver = drivers.get(i);
            rows[i] = new String[]{driver.getUsername(), driver.getAvailability() ? "Available" : "On a delivery",
                    driver.getRatings().isEmpty() ? "No ratings yet" : String.format(java.util.Locale.ROOT, "%.2f / 5", driver.getAverageRating()),
                    String.valueOf(driver.getRatings().size())};
        }
        page.add(scroll(table(rows, new String[]{"Driver", "Status", "Average", "Recent ratings"})), BorderLayout.CENTER);
        return page;
    }

    // Shows the driver's rating average and the ratings used to calculate it.
    private JPanel ratingsPage() {
        Driver driver = (Driver) system.getCurrentUser();
        JPanel page = card(30);
        page.add(label("Customer rating", 18, false, MUTED));
        page.add(Box.createVerticalStrut(20));
        page.add(label(driver.getRatings().isEmpty() ? "No ratings yet"
                : String.format(java.util.Locale.ROOT, "%.2f / 5", driver.getAverageRating()), 44, true, GREEN));
        page.add(Box.createVerticalStrut(28));
        page.add(label("Recent ratings, oldest first", 15, true, TEXT));
        page.add(Box.createVerticalStrut(12));
        page.add(label(driver.getRatings().toString(), 18, false, TEXT));
        page.add(Box.createVerticalStrut(24));
        page.add(label(driver.getAvailability() ? "You're available for a new order." : "You're currently on a delivery.",
                14, false, MUTED));
        return page;
    }

    // Opens account creation from the admin dashboard.
    private JPanel accountsPage() {
        JPanel page = card(30);
        page.add(label("Add someone to the system", 24, true, TEXT));
        page.add(Box.createVerticalStrut(12));
        page.add(label("Choose a role and create their login.", 15, false, MUTED));
        page.add(Box.createVerticalStrut(24));
        JButton add = button("Create an account", GREEN, Color.WHITE);
        add.setAlignmentX(Component.LEFT_ALIGNMENT);
        add.addActionListener(event -> attempt(() -> createAccount(true)));
        page.add(add);
        return page;
    }

    // Shows real activity, with customers and drivers limited to their own actions.
    private JPanel activityPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);
        try {
            ArrayList<ActivityEntry> activity = system.getActivity();
            String[][] rows = new String[activity.size()][5];
            for (int i = 0; i < activity.size(); i++) {
                ActivityEntry entry = activity.get(activity.size() - i - 1);
                String time = java.time.Instant.parse(entry.getTime()).atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"));
                rows[i] = new String[]{time, entry.getRole(), entry.getUsername(), entry.getEvent(), entry.getDetails()};
            }
            JTable table = table(rows, new String[]{"Time", "Role", "User", "Event", "Details"});
            table.getColumnModel().getColumn(0).setPreferredWidth(145);
            table.getColumnModel().getColumn(1).setPreferredWidth(85);
            table.getColumnModel().getColumn(2).setPreferredWidth(90);
            table.getColumnModel().getColumn(3).setPreferredWidth(90);
            table.getColumnModel().getColumn(4).setPreferredWidth(380);
            javax.swing.JTextArea details = new javax.swing.JTextArea(3, 20);
            details.setEditable(false);
            details.setLineWrap(true);
            details.setWrapStyleWord(true);
            details.setFont(font(13, false));
            details.setForeground(TEXT);
            details.setBackground(Color.WHITE);
            details.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            details.setText("Latest 200 visible events. Saved in activity.txt. Passwords are never logged.");
            table.getSelectionModel().addListSelectionListener(event -> {
                if (table.getSelectedRow() >= 0) {
                    String[] row = rows[table.convertRowIndexToModel(table.getSelectedRow())];
                    details.setText(row[0] + " | " + row[2] + " | " + row[3] + "\n" + row[4]);
                }
            });
            page.add(scroll(table), BorderLayout.CENTER);
            page.add(scroll(details), BorderLayout.SOUTH);
        } catch (IOException | java.time.format.DateTimeParseException ex) {
            page.add(label("Could not read activity: " + ex.getMessage(), 14, false, MUTED), BorderLayout.NORTH);
        }
        return page;
    }

    // Collects account details without letting public sign-up create an admin.
    private void createAccount(boolean admin) throws IOException {
        JPanel form = vertical(Color.WHITE, 12);
        JTextField username = field();
        JTextField email = field();
        JPasswordField password = new JPasswordField();
        styleField(password);
        JComboBox<String> role = new JComboBox<>(new String[]{"Customer", "Driver", "Admin"});
        if (admin) {
            addField(form, "Role", role);
        }
        addField(form, "Username", username);
        addField(form, "Email", email);
        addField(form, "Password (6 to 128 characters)", password);
        if (JOptionPane.showConfirmDialog(this, form, "Create account", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            system.register(admin ? (String) role.getSelectedItem() : "Customer", username.getText(),
                    new String(password.getPassword()), email.getText());
            if (admin) {
                notice = "Account created.";
                showDashboard("Accounts");
            } else {
                JOptionPane.showMessageDialog(this, "Your account is ready. You can sign in now.");
            }
        }
    }

    // Lets the logged-in user update their email and password.
    private void changeProfile(String section) throws IOException {
        JPanel form = vertical(Color.WHITE, 12);
        JTextField email = field();
        email.setText(system.getCurrentUser().getEmail());
        JPasswordField password = new JPasswordField();
        styleField(password);
        addField(form, "Email", email);
        addField(form, "New password (leave blank to keep)", password);
        if (JOptionPane.showConfirmDialog(this, form, "My profile", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            system.changeInfo(email.getText(), new String(password.getPassword()));
            notice = "Profile saved.";
            showDashboard(section);
        }
    }

    // Handles input errors and stops the app if saving fails.
    private void attempt(Action action) {
        try {
            action.run();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Check your input", JOptionPane.WARNING_MESSAGE);
        } catch (java.text.ParseException ex) {
            JOptionPane.showMessageDialog(this, "Enter a whole number from 1 to 5.", "Check your rating", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage()
                    + "\nThe app will close. Fix the file problem before reopening it.", "File error", JOptionPane.ERROR_MESSAGE);
            try {
                system.close();
            } catch (IOException closeError) {
                ex.addSuppressed(closeError);
            }
            dispose();
        }
    }

    // Returns a selected model row even after a table has been sorted.
    private int selectedRow(JTable table) {
        if (table.getSelectedRow() < 0) {
            throw new IllegalArgumentException("Select a row first.");
        }
        return table.convertRowIndexToModel(table.getSelectedRow());
    }

    // Builds a short item list for an order row.
    private String itemNames(List<MenuItem> items) {
        LinkedHashMap<String, Integer> quantities = new LinkedHashMap<>();
        for (MenuItem item : items) {
            quantities.merge(item.getName(), 1, Integer::sum);
        }
        ArrayList<String> names = new ArrayList<>();
        for (String name : quantities.keySet()) {
            names.add(quantities.get(name) + "x " + name);
        }
        return String.join(", ", names);
    }

    // Makes a read-only table with consistent fonts, spacing, and sorting.
    private JTable table(String[][] rows, String[] columns) {
        DefaultTableModel model = new DefaultTableModel(rows, columns) {
            private static final long serialVersionUID = 1L;

            // Makes changes go through dashboard actions instead of editable cells.
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setFont(font(13, false));
        table.setForeground(TEXT);
        table.setRowHeight(42);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(218, 234, 219));
        table.setSelectionForeground(TEXT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(12, 0));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setFont(font(12, true));
        table.getTableHeader().setBackground(new Color(232, 237, 228));
        table.getTableHeader().setForeground(GREEN);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        return table;
    }

    // Puts content in a scrollable area without heavy borders.
    private JScrollPane scroll(Component component) {
        JScrollPane scroll = new JScrollPane(component);
        if (component instanceof JTable table) {
            scroll.setColumnHeaderView(table.getTableHeader());
        }
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    // Creates a left-aligned row for page actions.
    private JPanel actions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panel.setOpaque(false);
        return panel;
    }

    // Creates a panel with vertically stacked content.
    private JPanel vertical(Color background, int padding) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return panel;
    }

    // Wraps major sections in a rounded white card.
    private JPanel card(int padding) {
        JPanel panel = new CardPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return panel;
    }

    // Adds a label and its field so keyboard and accessibility tools can identify it.
    private void addField(JPanel panel, String text, JComponent field) {
        JLabel label = label(text, 13, true, TEXT);
        label.setLabelFor(field);
        panel.add(label);
        panel.add(Box.createVerticalStrut(7));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setPreferredSize(new Dimension(300, 40));
        panel.add(field);
        panel.add(Box.createVerticalStrut(17));
    }

    // Creates a text field with the shared form style.
    private JTextField field() {
        JTextField field = new JTextField();
        styleField(field);
        return field;
    }

    // Styles both normal text fields and password fields.
    private void styleField(JTextField field) {
        field.setFont(font(15, false));
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    // Creates a flat button with enough padding for the desktop UI.
    private JButton button(String text, Color background, Color foreground) {
        JButton button = new RoundedButton(text);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.setFont(font(14, true));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    // Creates a consistently sized label without HTML formatting.
    private JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    // Uses a standard Java font that is available on Windows and macOS.
    private Font font(int size, boolean bold) {
        return new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size);
    }

    // Replaces the current screen and lets Swing lay it out again.
    private void replaceContent(JPanel root) {
        setContentPane(root);
        revalidate();
        repaint();
    }

    // Renders the real screen for layout checks without showing a window.
    void writePreview(java.nio.file.Path file, String section) throws IOException {
        if (system.getCurrentUser() == null) {
            showLogin();
        } else {
            showDashboard(section);
        }
        java.awt.Container content = getContentPane();
        content.setSize(1080, 692);
        layoutPreview(content);
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(2160, 1384,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.scale(2, 2);
        content.printAll(graphics);
        graphics.dispose();
        javax.imageio.ImageIO.write(image, "png", file.toFile());
    }

    // Lays out nested panels before an offscreen preview is drawn.
    private void layoutPreview(java.awt.Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof java.awt.Container nested) {
                layoutPreview(nested);
            }
        }
    }

    private static final class CardPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        // Keeps card corners transparent against the page background.
        private CardPanel() {
            setOpaque(false);
        }

        // Draws a soft card outline without changing the layout inside it.
        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            java.awt.Graphics2D copy = (java.awt.Graphics2D) graphics.create();
            copy.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setColor(getBackground());
            copy.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            copy.setColor(LINE);
            copy.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            copy.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class RoundedButton extends JButton {
        private static final long serialVersionUID = 1L;

        // Uses the same button shape on Windows and macOS.
        private RoundedButton(String text) {
            super(text);
            setRolloverEnabled(true);
        }

        // Draws hover, disabled, and keyboard-focus states.
        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            java.awt.Graphics2D copy = (java.awt.Graphics2D) graphics.create();
            copy.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            Color background = !isEnabled() ? new Color(231, 233, 228) : getBackground();
            if (isEnabled() && (getModel().isRollover() || getModel().isPressed())) {
                background = background.equals(Color.WHITE) ? BACKGROUND : background.brighter();
            }
            copy.setColor(background);
            copy.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            if (hasFocus()) {
                copy.setColor(ORANGE);
                copy.setStroke(new java.awt.BasicStroke(2));
                copy.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
            }
            copy.setFont(getFont());
            copy.setColor(isEnabled() ? getForeground() : MUTED);
            java.awt.FontMetrics metrics = copy.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(getText())) / 2;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            copy.drawString(getText(), x, y);
            copy.dispose();
        }
    }
}
