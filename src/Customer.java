public class Customer extends User {
    // Creates a customer from a saved user record.
    public Customer(String username, String passwordHash, String email) {
        super(username, passwordHash, email);
    }

    // Identifies the customer dashboard.
    @Override
    public String getRole() {
        return "Customer";
    }

    // Shows the options a customer can use.
    @Override
    public void viewOptions() {
        System.out.println("\n1. View menu\n2. Place order\n3. Order history"
                + "\n4. Rate delivery\n5. Cancel waiting order\n6. Change profile\n9. Activity\n0. Log out");
    }
}
