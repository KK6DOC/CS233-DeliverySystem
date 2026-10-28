import java.util.ArrayDeque;
import java.util.Deque;

public class Driver extends User implements Comparable<Driver> {
    private boolean availability = true;
    private final Deque<Integer> ratings = new ArrayDeque<>();
    private double averageRating;

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
    public double getAverageRating() {
        return averageRating;
    }

    // Returns a copy so callers cannot change the driver's ratings.
    public Deque<Integer> getRatings() {
        return new ArrayDeque<>(ratings);
    }

    // Adds a rating and drops the oldest one once there are more than ten.
    void addRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Ratings must be a whole number from 1 to 5.");
        }
        if (ratings.size() == 10) {
            ratings.removeFirst();
        }
        ratings.addLast(rating);
        calculateAvgRating();
    }

    // Restores a saved rating snapshot in oldest-to-newest order.
    void restoreRatings(Deque<Integer> saved) {
        if (saved.size() > 10) {
            throw new IllegalArgumentException("A driver cannot have more than ten ratings.");
        }
        for (int rating : saved) {
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("A saved rating is outside 1 to 5.");
            }
        }
        ratings.clear();
        ratings.addAll(saved);
        calculateAvgRating();
    }

    // Recalculates the average after the rating list changes.
    private void calculateAvgRating() {
        int total = 0;
        for (int rating : ratings) {
            total += rating;
        }
        averageRating = ratings.isEmpty() ? 0 : (double) total / ratings.size();
    }

    // Puts the highest-rated driver first, with username breaking ties.
    @Override
    public int compareTo(Driver other) {
        int result = Double.compare(other.averageRating, averageRating);
        return result == 0 ? getUsername().compareTo(other.getUsername()) : result;
    }
}
