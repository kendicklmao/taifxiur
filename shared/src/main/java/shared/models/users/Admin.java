package shared.models.users;

import shared.enums.Role;

// Quản trị viên
public class Admin extends User {
    public Admin(int id, String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(id, username, password, email, Role.ADMIN, q1, a1, q2, a2);
    }

    public Admin(String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(0, username, password, email, Role.ADMIN, q1, a1, q2, a2);
    }

    public Admin(int id, String username, String hashedPassword, String passwordSalt, String email, 
                 boolean isBanned, String q1, String hashedA1, String saltA1, String q2, String hashedA2, String saltA2) {
        super(id, username, hashedPassword, passwordSalt, email, Role.ADMIN, isBanned, q1, hashedA1, saltA1, q2, hashedA2, saltA2);
    }

    // Chặn người dùng
    public void ban(User user) {
        if (user != null) {
            if (user.getRole() == Role.ADMIN) {
                throw new IllegalArgumentException("Cannot ban an administrator");
            }
            user.banUser();
        }
    }

    // Bỏ chặn người dùng
    public void unban(User user) {
        if (user != null) {
            if (user.getRole() == Role.ADMIN) {
                throw new IllegalArgumentException("Cannot unban an administrator");
            }
            user.unbanUser();
        }
    }
}