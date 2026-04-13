package shared.models;

import shared.enums.Role;

public class Admin extends User { //quản trị viên
    public Admin(String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(username, password, email, Role.ADMIN, q1, a1, q2, a2);
    }

    public void ban(User user) { //chặn người dùng
        if (user != null) {
            user.banUser();
        }
    }

    public void unban(User user) { //bỏ chặn người dùng
        if (user != null) {
            user.unbanUser();
        }
    }
}