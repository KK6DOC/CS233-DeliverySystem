import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class Menu {
    private final HashMap<String, MenuItem> menu = new HashMap<>();

    // Adds an item and stops duplicate names, even with different capitalization.
    public void addMenuItem(MenuItem item) {
        String key = item.getName().toLowerCase(Locale.ROOT);
        if (menu.containsKey(key)) {
            throw new IllegalArgumentException("That item is already on the menu.");
        }
        menu.put(key, item);
    }

    // Replaces an existing item's price.
    public void updateMenu(MenuItem item) {
        getItem(item.getName());
        menu.put(item.getName().toLowerCase(Locale.ROOT), item);
    }

    // Removes an item from future orders.
    public void removeMenuItem(String name) {
        getItem(name);
        menu.remove(name.trim().toLowerCase(Locale.ROOT));
    }

    // Finds an item by its name.
    public MenuItem getItem(String name) {
        MenuItem item = menu.get(name.trim().toLowerCase(Locale.ROOT));
        if (item == null) {
            throw new IllegalArgumentException("That item is not on the menu.");
        }
        return item;
    }

    // Returns the menu in alphabetical order for the screen and saved file.
    public ArrayList<MenuItem> getItems() {
        ArrayList<MenuItem> items = new ArrayList<>(menu.values());
        items.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return items;
    }
}
