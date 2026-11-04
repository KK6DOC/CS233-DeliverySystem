import java.util.ArrayList;
import java.util.List;

public class Order {
    public enum Status { PLACED, ACCEPTED, IN_PROGRESS, DELIVERED, CANCELLED }

    private final int orderNum;
    private final String customer;
    private final ArrayList<MenuItem> items;
    private Status status;
    private String driver;
    private float rating;

    // Creates a waiting order with the prices that were shown at checkout.
    public Order(int orderNum, String customer, List<MenuItem> items) {
        this(orderNum, customer, items, Status.PLACED, "", 0);
    }

    // Restores a saved order or makes a copy for the UI.
    Order(int orderNum, String customer, List<MenuItem> items, Status status, String driver, float rating) {
        if (orderNum < 1 || items == null || items.isEmpty() || items.size() > 100) {
            throw new IllegalArgumentException("An order needs 1 to 100 items and a valid order number.");
        }
        if (status == null || driver == null || !Float.isFinite(rating)
                || (rating != 0 && (rating < 1 || rating > 5))) {
            throw new IllegalArgumentException("The order's saved status or rating is not valid.");
        }
        boolean needsDriver = status == Status.ACCEPTED || status == Status.IN_PROGRESS || status == Status.DELIVERED;
        if ((needsDriver && driver.isEmpty()) || (status == Status.PLACED && !driver.isEmpty())
                || (rating > 0 && status != Status.DELIVERED)) {
            throw new IllegalArgumentException("The order's driver does not match its status.");
        }
        this.orderNum = orderNum;
        this.customer = User.normalizeUsername(customer);
        this.items = new ArrayList<>(items);
        if (this.items.contains(null)) {
            throw new IllegalArgumentException("An order cannot contain an empty item.");
        }
        this.status = status;
        this.driver = driver.isEmpty() ? "" : User.normalizeUsername(driver);
        this.rating = rating;
    }

    // Returns the numeric order number.
    public int getOrderId() {
        return orderNum;
    }

    // Returns the customer who owns this order.
    public String getCustomer() {
        return customer;
    }

    // Returns a copy of the items in this order.
    public ArrayList<MenuItem> getItems() {
        return new ArrayList<>(items);
    }

    // Returns the current order status.
    public Status getStatus() {
        return status;
    }

    // Returns the assigned driver's username, or an empty string while waiting.
    public String getDriver() {
        return driver;
    }

    // Returns zero until the customer rates the delivery.
    public float getRating() {
        return rating;
    }

    // Totals the float checkout prices with the same calculation used by the cart.
    public float calculateTotalCost() {
        return NumberUtil.totalCost(items);
    }

    // Copies the order before making a change that needs to be saved.
    public Order copy() {
        return new Order(orderNum, customer, items, status, driver, rating);
    }

    // Assigns a waiting order to a driver.
    void assignDriver(String username) {
        requireStatus(Status.PLACED);
        driver = User.normalizeUsername(username);
        status = Status.ACCEPTED;
    }

    // Marks an accepted order as out for delivery.
    void startDelivery() {
        requireStatus(Status.ACCEPTED);
        status = Status.IN_PROGRESS;
    }

    // Finishes an order that is already out for delivery.
    void completeDelivery() {
        requireStatus(Status.IN_PROGRESS);
        status = Status.DELIVERED;
    }

    // Checks whether the customer can cancel before delivery starts.
    public boolean canCancel() {
        return status == Status.PLACED || status == Status.ACCEPTED;
    }

    // Cancels a waiting or accepted order and keeps its driver in the history.
    void cancel() {
        if (!canCancel()) {
            throw new IllegalArgumentException("Only waiting or accepted orders can be cancelled before delivery starts.");
        }
        status = Status.CANCELLED;
    }

    // Records one float rating after delivery.
    void rate(float rating) {
        requireStatus(Status.DELIVERED);
        if (this.rating != 0) {
            throw new IllegalArgumentException("This delivery has already been rated.");
        }
        NumberUtil.requireRating(rating);
        this.rating = rating;
    }

    // Stops status updates that skip or repeat a step.
    private void requireStatus(Status expected) {
        if (status != expected) {
            throw new IllegalArgumentException("This order is " + status + "; this action needs " + expected + ".");
        }
    }
}
