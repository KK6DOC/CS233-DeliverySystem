import java.io.*;
import java.util.*;

public class DeliverySystem {
    private Queue<Order> ordersQueue;
    private ArrayList<Order> allOrders;
    private Stack<String> readyDriverNames;
    private ArrayList<Driver> allDrivers;

    public DeliverySystem() {
        this.ordersQueue = new LinkedList<>();
        this.readyDriverNames = new Stack<>();
        this.allOrders = new ArrayList<>();
    }
    // Add order for new orders, presets driver to none, and status to pending
    public void addOrder(int orderNum, String customer, ArrayList<String> foodNames, ArrayList<Double> foodCost){
        allOrders.add(new Order(orderNum, customer, foodNames, foodCost));
        ordersQueue.add(new Order(orderNum, customer, foodNames, foodCost));
    }
    // Add order with driver and status preset.
    public void addOrder(int orderNum, String customer, String driverName, String status, double rating, ArrayList<String> foodNames, ArrayList<Double> foodCost) {
        // Create new Order Object based on arguments
        Order order = new Order(orderNum, customer, driverName, status, rating, foodNames, foodCost);
        // Add the order to our list, if the order is Pending, add it to the queue.
        allOrders.add(order);
        if(order.getStatus().equals("Pending")){
            ordersQueue.add(order);
        }
    }
    // Returns order based on ordernumber.
    public Order getOrder(int orderNumber){
        return allOrders.get(orderNumber);
    }

    // Create method to initialize ready drivers into Stack
    public void addDriver(Driver driver){

    }

}