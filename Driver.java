import java.util.*;

public class Driver extends User {

    private boolean availability;
    private LinkedList<Double> ratings;
    private double averageRating;

    // Creates driver object
    public Driver(String username, String password, String email) {
        super(username, password, email);
    }

    public boolean getAvailabiliy(){ return availability; }

    public void setAvailabiliy(boolean availability){ this.availability = availability; }

    public double getAverageRating(){
        calculateAvgRating();
        return averageRating;
    }

    public LinkedList<Double> getRatings(){ return ratings; }

    // Adds the last 10 ratings an average
    private void calculateAvgRating() {
        if (ratings == null || ratings.isEmpty()) {
            averageRating = 0.0;
            return;
        }
        double sum = 0;
        int count = 0;
        // Iterate though first 10 or whatever is less.
        for (int i = 0; i < Math.min(10, ratings.size()); i++) {
            if (ratings.get(i) != null) {
                sum = sum + ratings.get(i);
                count++;
            }
        }
        // Check for divide by 0
        averageRating = (count > 0) ? (sum / count) : 0.0;
    }
}
