package shared.models;

import shared.enums.Role;

public class Admin extends User { //quản trị viên
    public Admin(int id, String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(id, username, password, email, Role.ADMIN, q1, a1, q2, a2);
    }

    public Admin(String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(0, username, password, email, Role.ADMIN, q1, a1, q2, a2);
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