import java.util.*;

public class Order {
    private int orderNum;
    private String customer;
    private String driverName;
    // Three Statuses: Pending, Enroute, Delivered
    private String status;
    private ArrayList<String> foodNames;
    private ArrayList<Double> foodCost;

    public Order(int orderNum, String customer, ArrayList<String> foodNames, ArrayList<Double> foodCost) {
        this.orderNum = orderNum;
        this.customer = customer;
        this.driverName = "None";
        this.status = "Pending";
        this.foodNames = foodNames;
        this.foodCost = foodCost;
    }

    public Order(int orderNum, String customer, String driverName, String status, ArrayList<String> foodNames, ArrayList<Double> foodCost) {
        this.orderNum = orderNum;
        this.customer = customer;
        this.driverName = driverName;
        this.status = status;
        this.foodNames = foodNames;
        this.foodCost = foodCost;
    }
    
    public int getOrderId() { return orderNum; }
    public String getCustomer() { return customer; }
    public ArrayList<String> getItems() { return foodNames; }
    public ArrayList<Double> getCost() { return foodCost; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double calculateTotalCost() {
        double total = 0.0;
        for (double cost : foodCost) {
            total += cost;
        }
        return total;
    }
}