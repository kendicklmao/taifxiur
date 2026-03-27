public class Admin extends User {

    public Admin(String username, String password, String email,
                 String q1, String a1, String q2, String a2) {
        super(username, password, email, Role.ADMIN, q1, a1, q2, a2);
    }

    @Override
    public void displayInfo() {
        System.out.println("Admin: " + getUsername() + " | Email: " + getEmail());
    }

    public void ban(User user) {
        if (user != null) {
            user.banUser();
        }
    }

    public void unban(User user) {
        if (user != null) {
            user.unbanUser();
        }
    }
}