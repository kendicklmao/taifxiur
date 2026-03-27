public class Seller extends User {

    public Seller(String username, String password, String email,
                  String q1, String a1, String q2, String a2) {
        super(username, password, email, Role.SELLER, q1, a1, q2, a2);
    }

    @Override
    public void displayInfo() {
        System.out.println("Seller: " + getUsername() + " | Email: " + getEmail());
    }
}