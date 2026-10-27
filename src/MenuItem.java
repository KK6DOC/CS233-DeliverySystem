import java.math.BigDecimal;
import java.math.RoundingMode;

public class MenuItem {
    private final String name;
    private final BigDecimal price;

    // Stores a food name and its price in dollars and cents.
    public MenuItem(String name, BigDecimal price) {
        if (name == null || name.isBlank() || name.trim().length() > 60
                || name.chars().anyMatch(ch -> ch == '|' || ch == ',' || Character.isISOControl(ch))) {
            throw new IllegalArgumentException("Use 1 to 60 characters for food names, without commas, bars, or control characters.");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Price must be zero or more.");
        }
        try {
            this.price = price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Use no more than two decimal places for prices.");
        }
        this.name = name.trim();
    }

    // Returns the name shown on the menu.
    public String getName() {
        return name;
    }

    // Returns the price without changing the saved item.
    public BigDecimal getPrice() {
        return price;
    }
}
