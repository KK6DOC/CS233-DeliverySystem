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
