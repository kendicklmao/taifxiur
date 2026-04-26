package shared.models;

import shared.enums.Role;
import shared.utils.Hash;
import shared.utils.Validator;

public abstract class User {
    private final int id;
    private final Wallet wallet;
    private String hashedPassword; // Mật khẩu đã mã hóa
    private String passwordSalt; // Salt cho mật khẩu
    private final String username; // Tên tài khoản
    private String email; // Email
    private boolean isBanned;// Bị chặn hay không?
    private final Role role; // Loại tài khoản
    private final String securityQuestion1; // Câu hỏi xác nhận danh tính 1
    private String securityAnswer1; // Câu trả lời xác nhận danh tính 1 (hashed)
    private String answerSalt1; // Salt cho câu trả lời 1
    private final String securityQuestion2;// Câu hỏi xác nhận danh tính 2
    private String securityAnswer2; // Câu trả lời xác nhận danh tính 2 (hashed)
    private String answerSalt2; // Salt cho câu trả lời 2

    // Constructor dành cho việc ĐĂNG KÝ MỚI (Sẽ tự tạo Salt và Hash)
    public User(int id, String username, String password, String email, Role role, String q1, String a1, String q2,
            String a2) {
        this.id = id;
        this.username = Validator.normalizeAndLowercase(username);
        this.email = Validator.normalizeAndLowercase(email);
        this.role = role;
        this.isBanned = false;
        this.wallet = new Wallet();
        
        // Tạo Salt mới
        this.passwordSalt = Hash.generateSalt();
        this.answerSalt1 = Hash.generateSalt();
        this.answerSalt2 = Hash.generateSalt();

        // Hash dữ liệu
        this.hashedPassword = Hash.formula(Validator.normalize(password), passwordSalt);
        this.securityQuestion1 = Validator.normalize(q1);
        this.securityAnswer1 = Hash.formula(Validator.normalizeAndLowercase(a1), answerSalt1);
        this.securityQuestion2 = Validator.normalize(q2);
        this.securityAnswer2 = Hash.formula(Validator.normalizeAndLowercase(a2), answerSalt2);
    }

    // Constructor dành cho việc LOAD TỪ DATABASE (Nhận trực tiếp Hash và Salt đã lưu)
    public User(int id, String username, String hashedPassword, String passwordSalt, String email, Role role, 
                boolean isBanned, String q1, String hashedA1, String saltA1, String q2, String hashedA2, String saltA2) {
        this.id = id;
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.passwordSalt = passwordSalt;
        this.email = email;
        this.role = role;
        this.isBanned = isBanned;
        this.wallet = new Wallet();
        this.securityQuestion1 = q1;
        this.securityAnswer1 = hashedA1;
        this.answerSalt1 = saltA1;
        this.securityQuestion2 = q2;
        this.securityAnswer2 = hashedA2;
        this.answerSalt2 = saltA2;
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

    public Wallet getWallet() {
        return wallet;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getAnswerSalt1() {
        return answerSalt1;
    }

    public String getAnswerSalt2() {
        return answerSalt2;
    }

    public String getSecurityAnswer1() {
        return securityAnswer1;
    }

    public String getSecurityAnswer2() {
        return securityAnswer2;
    }

    public String getSecurityQuestion1() {
        return securityQuestion1;
    }

    public String getSecurityQuestion2() {
        return securityQuestion2;
    }

    public void setBanned(boolean banned) {
        this.isBanned = banned;
    }

    // Thay đổi mật khẩu
    public boolean setPassword(String newPassword) {
        newPassword = Validator.normalize(newPassword);
        if (!Validator.isValidPassword(newPassword)) {
            return false;
        }
        this.passwordSalt = Hash.generateSalt();
        this.hashedPassword = Hash.formula(newPassword, this.passwordSalt);
        return true;
    }

    // Thay đổi email
    public boolean setEmail(String email) {
        email = Validator.normalizeAndLowercase(email);
        if (!Validator.isValidEmail(email)) {
            return false;
        }
        this.email = email;
        return true;
    }

    // Chặn người dùng
    public void banUser() {
        this.isBanned = true;
    }

    // Bỏ chặn người dùng
    public void unbanUser() {
        this.isBanned = false;
    }

    // Kiểm tra danh tính
    public boolean verifySecurityAnswers(String a1, String a2) {
        a1 = Validator.normalizeAndLowercase(a1);
        a2 = Validator.normalizeAndLowercase(a2);
        String hashedA1 = Hash.formula(a1, answerSalt1);
        String hashedA2 = Hash.formula(a2, answerSalt2);
        return securityAnswer1.equals(hashedA1) && securityAnswer2.equals(hashedA2);
    }

    // Đổi mật khẩu nếu quên mật khẩu cũ
    public boolean resetPassword(String a1, String a2, String newPassword) {
        if (verifySecurityAnswers(a1, a2)) {
            return setPassword(newPassword);
        }
        return false;
    }

    // Đổi mật khẩu nếu nhớ mật khẩu cũ
    public boolean changePassword(String oldPassword, String newPassword) {
        if (!checkPassword(oldPassword)) {
            return false;
        }
        newPassword = Validator.normalize(newPassword);
        if (!Validator.isValidPassword(newPassword)) {
            return false;
        }
        this.passwordSalt = Hash.generateSalt();
        this.hashedPassword = Hash.formula(newPassword, passwordSalt);
        return true;
    }

    // Kiểm tra mật khẩu
    public boolean checkPassword(String inputPassword) {
        inputPassword = Validator.normalize(inputPassword);
        String hashInput = Hash.formula(inputPassword, this.passwordSalt);
        return this.hashedPassword.equals(hashInput);
    }
}