package server.service;

import server.database.DatabaseConfig;
import shared.enums.Role;
import shared.models.Admin;
import shared.models.Bidder;
import shared.models.Seller;
import shared.models.User;
import shared.utils.Validator;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
    private final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap<>(); //lưu số lần đăng nhập thất bại của từng tài khoản
    private final ConcurrentHashMap<String, Instant> lockUntil = new ConcurrentHashMap<>(); //lưu số giây bị ban của từng tài khoản
    private final ConcurrentHashMap<String, Boolean> loggedIn = new ConcurrentHashMap<>(); //lưu trạng thái đăng nhập
    private static final int MAX_ATTEMPTS = 5; //số lượt đăng nhập thất bại tối đa
    private static final long BASE_LOCK_SECONDS = 2; //số giây cơ sở để vô hiệu hóa nếu đăng nhập thất bại

    public UserService() {
        // Initialize default users in the database
        initializeDefaultUsers();
    }

    /**
     * Initialize default users in the database on first run
     */
    private void initializeDefaultUsers() {
        this.register("seller", "Admin@123", "seller@gmail.com", "q", "a", "q", "a", Role.SELLER);
        this.register("bidder", "Admin@123", "bidder@gmail.com", "q", "a", "q", "a", Role.BIDDER);
        this.register("admin1", "Admin@123", "admin@gmail.com", "q", "a", "q", "a", Role.ADMIN);
        this.register("bidder1", "Admin@123", "bidder1@gmail.com", "q", "a", "q", "a", Role.BIDDER);
    }

    /**
     * Register a new user in the database
     */
    public boolean register(String username, String password, String email, String q1, String a1, String q2, String a2, Role role) {
        if (!Validator.isValidUsername(username)) {
            System.out.println("Username sai định dạng r ngu");
            return false;
        }
        if (!Validator.isValidPassword(password)) {
            System.out.println("Password sai định dạng r ngu");
            return false;
        }
        if (!Validator.isValidEmail(email)) {
            System.out.println("Email sai định dạng r ngu");
            return false;
        }

        username = Validator.normalizeUsername(username);
        password = Validator.normalizePassword(password);
        email = Validator.normalizeEmail(email);
        q1 = Validator.normalizeQuestion(q1);
        q2 = Validator.normalizeQuestion(q2);
        a1 = Validator.normalizeAnswer(a1);
        a2 = Validator.normalizeAnswer(a2);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, email, role, question_1, answer_1, question_2, answer_2) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, email);
            pstmt.setString(4, role.name());
            pstmt.setString(5, q1);
            pstmt.setString(6, a1);
            pstmt.setString(7, q2);
            pstmt.setString(8, a2);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Đăng ký thành công vào sổ!");
                return true;
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate key")) {
                System.out.println("Username này đã tồn tại trong Sổ!");
            } else {
                System.err.println("Error registering user: " + e.getMessage());
            }
            return false;
        }
        return false;
    }

    /**
     * Login user and authenticate
     */
    public User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        username = Validator.normalizeUsername(username);
        password = Validator.normalizePassword(password);

        Instant now = Instant.now();
        if (lockUntil.containsKey(username)) {
            Instant unlockTime = lockUntil.get(username);
            if (now.isBefore(unlockTime)) {
                return null;
            } else {
                lockUntil.remove(username);
                failedAttempts.remove(username);
            }
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, password, role, is_banned FROM users WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            String storedPassword = rs.getString("password");
            boolean isBanned = rs.getBoolean("is_banned");
            String roleStr = rs.getString("role");
            int userId = rs.getInt("id");

            if (isBanned) {
                return null;
            }

            if (storedPassword.equals(password)) {
                if (loggedIn.putIfAbsent(username, true) != null) {
                    return null; // Already logged in
                }
                failedAttempts.remove(username);
                lockUntil.remove(username);
                return getUserFromDatabase(username);
            } else {
                int attempts = failedAttempts.getOrDefault(username, 0) + 1;
                failedAttempts.put(username, attempts);
                long lockSeconds;
                if (attempts >= MAX_ATTEMPTS) {
                    if (attempts > 11) {
                        lockSeconds = 3600;
                    } else {
                        lockSeconds = (long) Math.pow(BASE_LOCK_SECONDS, (attempts - 1));
                    }
                    lockUntil.put(username, now.plusSeconds(lockSeconds));
                }
                return null;
            }
        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get user from database by username
     */
    private User getUserFromDatabase(String username) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, username, password, email, role, is_banned, question_1, answer_1, question_2, answer_2 FROM users WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                String email = rs.getString("email");
                String password = rs.getString("password");
                String q1 = rs.getString("question_1");
                String a1 = rs.getString("answer_1");
                String q2 = rs.getString("question_2");
                String a2 = rs.getString("answer_2");
                boolean isBanned = rs.getBoolean("is_banned");

                User user = null;
                if ("BIDDER".equals(role)) {
                    user = new Bidder(username, password, email, q1, a1, q2, a2);
                } else if ("SELLER".equals(role)) {
                    user = new Seller(username, password, email, q1, a1, q2, a2);
                } else if ("ADMIN".equals(role)) {
                    user = new Admin(username, password, email, q1, a1, q2, a2);
                }

                if (user != null && isBanned) {
                    user.banUser();
                }

                return user;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if user exists
     */
    public boolean exists(String username) {
        if (username == null) return false;
        username = Validator.normalizeUsername(username);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking user existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get user by username
     */
    public User getUser(String username) {
        return getUserFromDatabase(username);
    }

    /**
     * Logout user
     */
    public void logout(String username) {
        loggedIn.remove(username);
    }

    /**
     * Get all users from database
     */
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username FROM users")) {

            while (rs.next()) {
                User user = getUserFromDatabase(rs.getString("username"));
                if (user != null) {
                    userList.add(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }

        return userList;
    }

    /**
     * Ban user
     */
    public boolean banUser(String username) {
        username = Validator.normalizeUsername(username);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET is_banned = TRUE WHERE username = ?")) {

            pstmt.setString(1, username);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error banning user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Unban user
     */
    public boolean unbanUser(String username) {
        username = Validator.normalizeUsername(username);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET is_banned = FALSE WHERE username = ?")) {

            pstmt.setString(1, username);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error unbanning user: " + e.getMessage());
            return false;
        }
    }
}