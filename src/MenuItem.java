public final class MenuItem {
    private final String name;
    private final float price;

    // Stores a food name and its price in dollars and cents.
    public MenuItem(String name, float price) {
        if (name == null || name.isBlank() || name.trim().length() > 60
                || name.chars().anyMatch(ch -> ch == '|' || ch == ',' || Character.isISOControl(ch))) {
            throw new IllegalArgumentException("Use 1 to 60 characters for food names, without commas, bars, or control characters.");
        }
        this.price = NumberUtil.requirePrice(price);
        this.name = name.trim();
    }

    // Returns the name shown on the menu.
    public String getName() {
        return name;
    }

    // Returns the price without changing the saved item.
    public float getPrice() {
        return price;
    }
}
