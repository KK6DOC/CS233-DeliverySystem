import java.util.HashMap;
import java.util.Map;

public class Menu {
    
    private HashMap<String, MenuItem> menu;

    public Menu() {
        this.menu = new HashMap<>();
    }

    public void addMenuItem(String foodName, double price, String description) {
        menu.put(foodName, new MenuItem(foodName, price, description));
        System.out.println("INFO: Added " + foodName);
    }
    
    public void removeMenuItem(String foodName){
        if (menu.remove(foodName) != null) { 
            System.out.println("INFO: Removed " + foodName);    
        } else {
            System.out.println("INFO: Not Found ");    
        }
    }
    
    public void updateMenuItem(String foodName, double price, String description){
        MenuItem item = menu.get(foodName);
        if (item != null){
            item.setPrice(price);
            item.setDescription(description);
            System.out.println("INFO: Updated " + foodName);
        } else {
            System.out.println("INFO: Not Found ");
        }
    }

    public double getItemPrice(String foodName) {
        MenuItem item = menu.get(foodName);
        if (item != null){
            return item.getPrice();
        } else {
            return 0.0;
        }
    }

    public MenuItem getMenuItem(String foodName) {
        return menu.get(foodName);
    }

    public void viewMenuItems() {
        if (menu.isEmpty()) {
            System.out.println("Menu Empty.");
            return;
        }
        System.out.println("Resturant Menu:");
        for (Map.Entry<String, MenuItem> entry : menu.entrySet()) {
            MenuItem item = entry.getValue();
            System.out.println(item.getName() + " " + item.getPrice() + " " + item.getDescription());
        }
    }
}
