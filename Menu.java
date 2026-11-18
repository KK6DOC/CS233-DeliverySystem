import java.awt.MenuItem;
import java.util.HashMap;

public class Menu {
    
    private HashMap<String, MenuItem> menu;

    public Menu() {
        this.menu = new HashMap<>();
    }

    public void addMenuItem(String foodName, double price, String description) {
        menu.put(foodName, new MenuItem(foodName, price, description));
        System.out.println("TEST: Added ");
    }
    
    public void removeMenuItem(String foodname){
        if (menu.remove(foodname) != null) { 
            System.out.println("TEST: Removed ");    
        } else {
            System.out.println("TEST: Not Found ");    
        }
    }
    
    public void updateMenuItem(String foodName, double price, String description){
        MenuItem item = menu.get(foodName);
        if (item != null){
            item.setPrice(price);
            item.setDescription(description);
            System.out.println("TEST: Updated ");
        } else {
            System.out.println("TEST: Not Found ");
        }
    }
}
