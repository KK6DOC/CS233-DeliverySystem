import java.util.Locale;
import java.util.List;

public final class NumberUtil {
    // Keeps this helper class from being used as an object.
    private NumberUtil() {
    }

    // Reads a price and gives both interfaces the same validation message.
    public static float parsePrice(String text) {
        try {
            return requirePrice(Float.parseFloat(text.trim()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a price such as 8.99.");
        }
    }

    // Checks that a price is finite, nonnegative, and has no fractions of a cent.
    public static float requirePrice(float price) {
        if (!Float.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("Price must be a finite number of zero or more.");
        }
        if (Float.compare(price, roundMoney(price)) != 0) {
            throw new IllegalArgumentException("Use no more than two decimal places for prices.");
        }
        return price;
    }

    // Rounds a computed money amount to cents while keeping its type as float.
    public static float roundMoney(float amount) {
        return Float.parseFloat(formatMoney(amount));
    }

    // Adds rounded float cents so repeated dollar-price additions do not build up small errors.
    public static float totalCost(List<MenuItem> items) {
        float cents = 0.0f;
        for (MenuItem item : items) {
            cents += (float) Math.rint(item.getPrice() * 100.0f);
        }
        return roundMoney(cents / 100.0f);
    }

    // Displays and saves money with two decimal places regardless of the computer's locale.
    public static String formatMoney(float amount) {
        if (!Float.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("The money amount is outside the supported float range.");
        }
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    // Reads a decimal rating such as 4.5 and checks its range.
    public static float parseRating(String text) {
        try {
            float rating = Float.parseFloat(text.trim());
            requireRating(rating);
            return rating;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a rating from 1 to 5, such as 4.5.");
        }
    }

    // Stops NaN, infinity, and ratings outside 1 to 5 from reaching the order or driver.
    public static void requireRating(float rating) {
        if (!Float.isFinite(rating) || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Ratings must be a number from 1 to 5.");
        }
    }
}
