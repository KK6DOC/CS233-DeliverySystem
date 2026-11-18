public class Admin extends User {
    public Admin(String username, String password, String email) {
        super(username, password, email);
    }

    @Override
    public void viewOptions() {
        System.out.println("--- ADMIN OPTIONS ---");
        System.out.println("1. Add Menu Item");
        System.out.println("2. Remove Menu Item");
        System.out.println("3. Update Menu Item");
        System.out.println("4. View All Orders");
        System.out.println("5. Logout");
    }
}