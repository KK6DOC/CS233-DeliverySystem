public class Admin extends User {
    // Creates an admin from a saved user record.
    public Admin(String username, String passwordHash, String email) {
        super(username, passwordHash, email);
    }

    // Identifies the admin dashboard.
    @Override
    public String getRole() {
        return "Admin";
    }

    // Shows the options an admin can use.
    @Override
    public void viewOptions() {
        System.out.println("\n1. View menu\n2. Add item\n3. Update item\n4. Remove item"
                + "\n5. All orders\n6. Add user\n7. View drivers\n8. Change profile\n9. Activity\n0. Log out");
    }
}
