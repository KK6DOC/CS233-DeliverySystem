import java.io.*;
import java.util.*;

public class FileManager {

    private String menuFile = "Menu.txt";
    private String userFile = "UserInfo.txt";
    private String orderFile = "Orders.txt"; 

    public FileManager(){
    }

    public void initializeMenu(Menu menu){
        File file = new File(menuFile);
        if (file.exists()){
            try (BufferedReader reader = new BufferedReader(new FileReader(file))){
                String line;
                while ((line = reader.readLine()) != null){
                    String[] item = line.split("\\|");
                    menu.addMenuItem(item[0],Double.parseDouble(item[1]),item[2]);
                }
                System.out.println("INFO: Menu loaded ");

            } catch (IOException e) {
                System.out.println("ERROR: Menu.txt BORK " + e.getMessage());
            }
        } else {
            System.out.println("ERROR: No menu file ");
        }
        return;
    }

    public User loginChecker(String username, String password) {
        File file = new File(userFile);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                    String un = parts[0];
                    String pw = parts[1];
                    String em = parts[2];
                    String rl = parts[3];

                    if (un.equals(username) && pw.equals(password)) {
                        if (rl.equals("Admin")) {
                            return new Admin(un, pw, em);
                        }
                        if (rl.equals("Driver")) {
                            return new Driver(un, pw, em);
                        }
                        if (rl.equals("Customer")) {
                            return new Customer(un, pw, em);
                        }
                    }
                }
            } catch (IOException e) {
            System.out.println("ERROR: UserInfo.txt BORK");
            } 
        } else {
            System.out.println("ERROR: No user file ");
        }
        System.out.println("ERROR: Couldn't Login");
        return null;
    }

    public void initalizeDeliverySystem(DeliverySystem deliverySystem){
        File file = new File(orderFile);
        if (file.exists()){
            try (BufferedReader reader = new BufferedReader(new FileReader(file))){
                String line;
                while ((line = reader.readLine()) != null){
                    String[] item = line.split("\\|");

                    ArrayList<String> itemName = new ArrayList<>();
                    for(int i = 4; i < item.length; i = i + 2){
                        itemName.add(item[i]);

                    }

                    ArrayList<Double> itemCost = new ArrayList<>();
                    for(int i = 5; i < item.length; i = i + 2){
                        itemCost.add(Double.parseDouble(item[i]));
                    }

                    deliverySystem.addOrder(Integer.parseInt(item[0]), item[1], item[2], item[3], itemName, itemCost);
                }
                System.out.println("INFO: Orders loaded ");

            } catch (IOException e) {
                System.out.println("ERROR: Orders.txt BORK " + e.getMessage());
            }
        } else {
            System.out.println("ERROR: No orders file ");
        }
        return;
    }
}
