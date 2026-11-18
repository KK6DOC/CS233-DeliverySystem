import java.io.*;
import java.util.*;

public class DeliverySystem {
    private Queue<Order> ordersQueue;
    private Stack<String> readyDriverNames;
    private ArrayList<Order> allOrders;

    public DeliverySystem() {
        this.ordersQueue = new LinkedList<>();
        this.readyDriverNames = new Stack<>();
        this.allOrders = new ArrayList<>();
    }

    public void addOrder(int orderNum, String customer, ArrayList<String> foodNames, ArrayList<Double> foodCost){
        allOrders.add(new Order(orderNum, customer, foodNames, foodCost));
    }

    public void addOrder(int orderNum, String customer, String driverName, String status, ArrayList<String> foodNames, ArrayList<Double> foodCost) {
        
        Order order = new Order(orderNum, customer, driverName, status, foodNames, foodCost);
        
        allOrders.add(order);
        if(order.getStatus().equals("Pending")){
            ordersQueue.add(order);
        }
    }

    public Order getOrder(int orderNumber){
        return allOrders.get(orderNumber);
    }


}