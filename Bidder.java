public class Bidder extends User {

    public Bidder(String username, String password, String email,
                  String q1, String a1, String q2, String a2) {
        super(username, password, email, Role.BIDDER, q1, a1, q2, a2);
    }

    @Override
    public void displayInfo() {
        System.out.println("Bidder: " + getUsername() + " | Email: " + getEmail());
    }
}