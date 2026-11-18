public class Main {
    public static void main(String[] args) {
        // Order of things to be done when the program starts.
        // 1. Start the File manager so items can be loaded and saved.
        // 2. initalize the menu with all items from Menu.txt
            // Here we can test if a generic item is found.
        // 3. Initalize the Delivery System, with all items from Orders.txt
        // 4. Launch the  CLI, so a user can now interact.

        // 1:
        FileManager fileSystem = new FileManager();
        // 2:
        Menu menu = new Menu();
        fileSystem.intializeMenu(menu);
        if (menu.getMenuItem("Burgor") != null) {
            System.out.println("INFO: Generic Menu Item Present");
        } else {
            System.out.println("INFO: Generic Menu Item Missing");
        }
    }
}
