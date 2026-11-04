import java.util.ArrayDeque;
import java.util.Deque;

public class Driver extends User implements Comparable<Driver> {
    private boolean availability = true;
    private final Deque<Float> ratings = new ArrayDeque<>();
    private float averageRating;

    // Creates a driver with no deliveries or ratings yet.
    public Driver(String username, String passwordHash, String email) {
        super(username, passwordHash, email);
    }

    // Identifies the driver dashboard.
    @Override
    public String getRole() {
        return "Driver";
    }

    // Shows the options a driver can use.
    @Override
    public void viewOptions() {
        System.out.println("\n1. Assigned orders\n2. Start delivery\n3. Complete delivery"
                + "\n4. View ratings\n5. Change profile\n9. Activity\n0. Log out");
    }

    // Returns whether this driver can take another order.
    public boolean getAvailability() {
        return availability;
    }

    // Updates availability when an order starts or finishes.
    void setAvailability(boolean availability) {
        this.availability = availability;
    }

    // Returns the average of the stored ratings, or zero when there are none.
    public float getAverageRating() {
        return averageRating;
    }

    // Returns a copy so callers cannot change the driver's ratings.
    public Deque<Float> getRatings() {
        return new ArrayDeque<>(ratings);
    }

    // Adds a rating and drops the oldest one once there are more than ten.
    void addRating(float rating) {
        NumberUtil.requireRating(rating);
        if (ratings.size() == 10) {
            ratings.removeFirst();
        }
        ratings.addLast(rating);
        calculateAvgRating();
    }

    // Restores a saved rating snapshot in oldest-to-newest order.
    void restoreRatings(Deque<Float> saved) {
        if (saved.size() > 10) {
            throw new IllegalArgumentException("A driver cannot have more than ten ratings.");
        }
        for (float rating : saved) {
            NumberUtil.requireRating(rating);
        }
        ratings.clear();
        ratings.addAll(saved);
        calculateAvgRating();
    }

    // Recalculates the average after the rating list changes.
    private void calculateAvgRating() {
        float total = 0;
        for (float rating : ratings) {
            total += rating;
        }
        averageRating = ratings.isEmpty() ? 0.0f : total / ratings.size();
    }

    // Puts the highest-rated driver first, with username breaking ties.
    @Override
    public int compareTo(Driver other) {
        int result = Float.compare(other.averageRating, averageRating);
        return result == 0 ? getUsername().compareTo(other.getUsername()) : result;
    }
}
