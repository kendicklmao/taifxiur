package shared.models;

import shared.enums.Role;
import shared.utils.hash;
import shared.utils.Validator;

public abstract class User {
    private int id;
    private String hashedPassword; //mật khẩu đã mã hóa
    private String passwordSalt; //String ngẫu nhiên làm rối loạn mã hóa cho mật khẩu
    private final String answerSalt1;  //String ngẫu nhiên làm rối loạn mã hóa cho câu trả lời xác nhận danh tính 1
    private final String answerSalt2; //String ngẫu nhiên làm rối loạn mã hóa cho câu trả lời xác nhận danh tính 2
    private final String username; //tên tài khoản
    private String email; //email
    private boolean isBanned;//bị chặn hay không?
    private final Role role; //loại tài khoản
    private final String securityQuestion1; //câu hỏi xác nhận danh tính 1
    private final String securityAnswer1; //câu trả lời xác nhận danh tính 1
    private final String securityQuestion2;//câu hỏi xác nhận danh tính 2
    private final String securityAnswer2; //câu trả lời xác nhận danh tính 2

    public User(int id, String username, String password, String email, Role role, String q1, String a1, String q2, String a2) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.isBanned = false;
        this.answerSalt1 = hash.formula(hash.generateSalt(), "");
        this.answerSalt2 = hash.formula(hash.generateSalt(), "");
        this.securityQuestion1 = q1;
        this.securityAnswer1 = hash.formula(a1, answerSalt1);
        this.securityQuestion2 = q2;
        this.securityAnswer2 = hash.formula(a2, answerSalt2);
        this.passwordSalt = hash.generateSalt();
        this.hashedPassword = hash.formula(password, passwordSalt);
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public Role getRole() {
        return role;
    }

    public String getSecurityQuestion1() {
        return securityQuestion1;
    }

    public String getSecurityQuestion2() {
        return securityQuestion2;
    }

    public boolean setPassword(String newPassword) {//thay đổi mật khẩu
        newPassword = Validator.normalizePassword(newPassword);
        if (!Validator.isValidPassword(newPassword)) {
            return false;
        }
        this.passwordSalt = hash.generateSalt();
        this.hashedPassword = hash.formula(newPassword, this.passwordSalt);
        return true;
    }

    public boolean setEmail(String email) {
        email = Validator.normalizeEmail(email);
        if (!Validator.isValidEmail(email)) {
            return false;
        }
        this.email = email;
        return true;
    }

    public void banUser() { //chặn người dùng
        this.isBanned = true;
    }

    public void unbanUser() { //bỏ chặn người dùng
        this.isBanned = false;
    }

    public boolean verifySecurityAnswers(String a1, String a2) { //kiểm tra danh tính
        a1 = Validator.normalizeAnswer(a1);
        a2 = Validator.normalizeAnswer(a2);
        String hashedA1 = hash.formula(a1, answerSalt1);
        String hashedA2 = hash.formula(a2, answerSalt2);
        return securityAnswer1.equals(hashedA1) && securityAnswer2.equals(hashedA2);
    }

    public boolean resetPassword(String a1, String a2, String newPassword) { //đổi mật khẩu nếu quên mật khẩu cũ
        if (verifySecurityAnswers(a1, a2)) {
            return setPassword(newPassword);
        }
        return false;
    }

    public boolean checkPassword(String inputPassword) { //kiểm tra mật khẩu
        inputPassword = Validator.normalizePassword(inputPassword);
        String hashInput = hash.formula(inputPassword, this.passwordSalt);
        return this.hashedPassword.equals(hashInput);
    }

    public boolean changePassword(String oldPassword, String newPassword) { //đổi mật khẩu nếu nhớ mật khẩu cũ
        if (!checkPassword(oldPassword)) return false;
        newPassword = Validator.normalizePassword(newPassword);
        if (!Validator.isValidPassword(newPassword)) return false;
        this.passwordSalt = hash.generateSalt();
        this.hashedPassword = hash.formula(newPassword, passwordSalt);
        return true;
    }
}