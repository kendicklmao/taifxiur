package server.service;

import server.database.DatabaseConfig;
import shared.enums.Role;
import shared.models.*;
import shared.models.users.Admin;
import shared.models.users.Bidder;
import shared.models.users.Seller;
import shared.models.users.User;
import shared.utils.Validator;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
    private static final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap<>(); // Lưu số lần đăng nhập thất bại của từng tài khoản
    private static final ConcurrentHashMap<String, Instant> lockUntil = new ConcurrentHashMap<>(); // Lưu số giây bị ban của từng tài khoản
    private static final ConcurrentHashMap<String, Boolean> loggedIn = new ConcurrentHashMap<>(); // Lưu trạng thái đăng nhập
    private static final int MAX_ATTEMPTS = 5; // Số lượt đăng nhập thất bại tối đa
    private static final long BASE_LOCK_SECONDS = 2; // Số giây cơ sở để vô hiệu hóa nếu đăng nhập thất bại
    // Khởi tạo người dùng mặc định trong cơ sở dữ liệu
    public UserService() {
    }

    public void initializeDefaultUsers() {
        if (!exists("admin")) {
            register("admin", "Admin@123", "admin@example.com", "q1", "a1", "q2", "a2", Role.ADMIN);
        }

        if (!exists("bidder")) {
            register("bidder", "Bidder@123", "bidder@example.com", "q1", "a1", "q2", "a2", Role.BIDDER);
        }

        if (!exists("seller")) {
            register("seller", "Seller@123", "seller@example.com", "q1", "a1", "q2", "a2", Role.SELLER);
        }
    }

    // Đăng ký người dùng mới trong cơ sở dữ liệu
    public boolean register(String username, String password, String email, String q1, String a1, String q2, String a2,
            Role role) {
        System.out.println("DEBUG REGISTER: Attempting to register user: " + username);
        if (!Validator.isValidUsername(username)) {
            System.out.println("DEBUG REGISTER: Invalid username format: " + username);
            return false;
        }

        if (!Validator.isValidPassword(password)) {
            System.out.println("DEBUG REGISTER: Invalid password format");
            return false;
        }

        if (!Validator.isValidEmail(email)) {
            System.out.println("DEBUG REGISTER: Invalid email format: " + email);
            return false;
        }

        username = Validator.normalizeAndLowercase(username);
        password = Validator.normalize(password);
        email = Validator.normalizeAndLowercase(email);
        q1 = Validator.normalize(q1);
        q2 = Validator.normalize(q2);
        a1 = Validator.normalizeAndLowercase(a1);
        a2 = Validator.normalizeAndLowercase(a2);

        System.out.println("DEBUG REGISTER: After normalization - username: " + username + ", email: " + email);

        // Kiểm tra xem tên người dùng đã tồn tại chưa
        if (exists(username)) {
            System.out.println("DEBUG REGISTER: Username already exists: " + username);
            return false;
        }

        // Kiểm tra xem email đã tồn tại chưa
        if (emailExists(email)) {
            System.out.println("DEBUG REGISTER: Email already exists: " + email);
            return false;
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO users (username, password, password_salt, email, role, question_1, answer_1, answer_salt_1, question_2, answer_2, answer_salt_2) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {

            // Tạo đối tượng User tạm thời để băm mật khẩu và tạo Salt
            User tempUser;
            if (role == Role.BIDDER) {
                tempUser = new Bidder(username, password, email, q1, a1, q2, a2);
            } else if (role == Role.SELLER) {
                tempUser = new Seller(username, password, email, q1, a1, q2, a2);
            } else {
                tempUser = new Admin(username, password, email, q1, a1, q2, a2);
            }

            pstmt.setString(1, username);
            pstmt.setString(2, tempUser.getHashedPassword());
            pstmt.setString(3, tempUser.getPasswordSalt());
            pstmt.setString(4, email);
            pstmt.setString(5, role.name());
            pstmt.setString(6, q1);
            pstmt.setString(7, tempUser.getSecurityAnswer1());
            pstmt.setString(8, tempUser.getAnswerSalt1());
            pstmt.setString(9, q2);
            pstmt.setString(10, tempUser.getSecurityAnswer2());
            pstmt.setString(11, tempUser.getAnswerSalt2());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("DEBUG REGISTER: User registered successfully!");
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    ensureWalletExists(conn, generatedKeys.getInt(1));
                }

                return true;
            }

        } catch (SQLException e) {
            System.out.println("DEBUG REGISTER: SQL Exception - " + e.getMessage());
            if (e.getMessage().contains("duplicate key")) {
                System.out.println("Duplicate key error - username or email already exists!");
            } else {
                System.err.println("Error registering user: " + e.getMessage());
                e.printStackTrace();
            }

            return false;
        }

        return false;
    }

    // Đăng nhập người dùng và xác thực
    public synchronized User login(String username, String password) {
        System.out.println("DEBUG LOGIN: Start for user " + username);
        if (username == null || password == null) {
            System.out.println("DEBUG LOGIN: Null username or password");
            return null;
        }

        username = Validator.normalizeAndLowercase(username);
        password = Validator.normalize(password);

        System.out.println("DEBUG LOGIN: Normalized user: " + username);
        Instant now = Instant.now();
        if (lockUntil.containsKey(username)) {
            Instant unlockTime = lockUntil.get(username);
            if (now.isBefore(unlockTime)) {
                long secondsRemaining = java.time.Duration.between(now, unlockTime).getSeconds();
                throw new UserLockedException(secondsRemaining);
            } else {
                lockUntil.remove(username);
            }
        }

        System.out.println("DEBUG LOGIN: Getting connection and preparing statement...");
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT id, password, password_salt, role, is_banned, is_online FROM users WHERE username = ?")) {

            pstmt.setString(1, username);
            System.out.println("DEBUG LOGIN: Executing query...");
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                System.out.println("DEBUG LOGIN: User not found in DB: " + username);
                return null;
            }

            boolean isOnline = rs.getBoolean("is_online");

            if (isOnline) {
                throw new UserAlreadyLoggedInException(
                        "User is already logged in from another device");
            }

            System.out.println("DEBUG LOGIN: User found in DB. Verifying password...");

            String storedHash = rs.getString("password");
            String storedSalt = rs.getString("password_salt");
            String roleStr = rs.getString("role");
            int userId = rs.getInt("id");

            String inputHash = shared.utils.Hash.formula(password, storedSalt); // Kiểm tra mật khẩu bằng cách băm thử với Salt đã lưu
            if (storedHash.equals(inputHash)) {
                System.out.println("DEBUG LOGIN: Password correct for " + username);
                try (PreparedStatement updateStmt =
                             conn.prepareStatement(
                                     "UPDATE users SET is_online = TRUE WHERE username = ?")) {

                    updateStmt.setString(1, username);
                    updateStmt.executeUpdate();
                }
                // Kiểm tra xem user đã đăng nhập chưa (tránh đăng nhập 2 lần)
                if (loggedIn.putIfAbsent(username, true) != null) {
                    System.out.println("DEBUG LOGIN: User already logged in!");
                    throw new UserAlreadyLoggedInException(
                            "User " + username + " is already logged in from another device");
                }

                // Đặt lại số lần thử và xóa khóa khi đăng nhập thành công
                failedAttempts.remove(username);
                lockUntil.remove(username);

                // Lưu lịch sử đăng nhập của admin
                if ("ADMIN".equals(roleStr)) {
                    logAdminLogin(userId, "SUCCESS");
                }

                System.out.println("DEBUG LOGIN: Fetching user details...");
                User result = getUserFromDatabase(username);
                System.out.println("DEBUG LOGIN: Login successful for " + username);
                return result;
            } else {
                int attempts = failedAttempts.getOrDefault(username, 0) + 1;
                failedAttempts.put(username, attempts);
                long lockSeconds;
                if (attempts >= MAX_ATTEMPTS) {
                    if (attempts >= 11) {
                        lockSeconds = 3600;
                    } else {
                        lockSeconds = (long) Math.pow(BASE_LOCK_SECONDS, (attempts - 1));
                    }

                    lockUntil.put(username, now.plusSeconds(lockSeconds));
                }

                if ("ADMIN".equals(roleStr)) {
                    logAdminLogin(userId, "FAILED");
                }

                return null;
            }

        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
            return null;
        }
    }

    private void logAdminLogin(int userId, String status) {
        String sql = "INSERT INTO admin_logs (user_id, status) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, status);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging admin login: " + e.getMessage());
        }
    }

    public boolean isUserBanned(String username) {
        if (username == null) {
            return false;
        }

        username = Validator.normalizeAndLowercase(username);
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT is_banned FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getBoolean("is_banned");
        } catch (SQLException e) {
            System.err.println("Error checking if user is banned: " + e.getMessage());
            return false;
        }
    }

    // Lấy thông tin người dùng từ cơ sở dữ liệu theo tên người dùng
    private User getUserFromDatabase(String username) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String role = rs.getString("role");
                String email = rs.getString("email");
                String hashedPassword = rs.getString("password");
                String passwordSalt = rs.getString("password_salt");
                String q1 = rs.getString("question_1");
                String hashedA1 = rs.getString("answer_1");
                String saltA1 = rs.getString("answer_salt_1");
                String q2 = rs.getString("question_2");
                String hashedA2 = rs.getString("answer_2");
                String saltA2 = rs.getString("answer_salt_2");
                boolean isBanned = rs.getBoolean("is_banned");
                boolean isOnline = rs.getBoolean("is_online");

                User user = null;
                if ("BIDDER".equals(role)) {
                    user = new Bidder(id, username, hashedPassword, passwordSalt, email, isBanned, q1, hashedA1, saltA1,
                            q2, hashedA2, saltA2);
                } else if ("SELLER".equals(role)) {
                    user = new Seller(id, username, hashedPassword, passwordSalt, email, isBanned, q1, hashedA1, saltA1,
                            q2, hashedA2, saltA2);
                } else if ("ADMIN".equals(role)) {
                    user = new Admin(id, username, hashedPassword, passwordSalt, email, isBanned, q1, hashedA1, saltA1,
                            q2, hashedA2, saltA2);
                }

                if (user != null) {
                    user.setOnline(isOnline);
                    if (isBanned) {
                        user.banUser();
                    }
                }

                loadWalletBalance(conn, user);
                return user;
            }

        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
        }

        return null;
    }

    // Lấy thông tin người dùng theo tên người dùng
    public User getUser(String username) {
        return getUserFromDatabase(username);
    }

    // Kiểm tra xem email đã tồn tại chưa
    public boolean emailExists(String email) {
        if (email == null) {
            return false;
        }

        email = Validator.normalizeAndLowercase(email);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM users WHERE email = ?")) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
            return false;
        }
    }

    // Kiểm tra xem người dùng có tồn tại không
    public boolean exists(String username) {
        if (username == null) {
            return false;
        }

        username = Validator.normalizeAndLowercase(username);

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

    // Kiểm tra xem người dùng có bị cấm không
    public boolean isBanned(String username) {
        if (username == null) {
            return false;
        }

        username = Validator.normalizeAndLowercase(username);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT is_banned FROM users WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_banned");
            }

            return false;
        } catch (SQLException e) {
            System.err.println("Error checking if user is banned: " + e.getMessage());
            return false;
        }
    }

    // Lấy câu trả lời bảo mật cho tên người dùng/email
    public String[] getSecurityQuestions(String username, String email) {
        if (!Validator.isValidUsername(username) || !Validator.isValidEmail(email)) {
            return null;
        }

        username = Validator.normalizeAndLowercase(username);
        email = Validator.normalizeAndLowercase(email);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT question_1, question_2 FROM users WHERE username = ? AND email = ?")) {

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new String[] {
                        rs.getString("question_1"), rs.getString("question_2")
                };
            }

        } catch (SQLException e) {
            System.err.println("Error fetching security questions: " + e.getMessage());
        }

        return null;
    }

    // Reset mật khẩu sau khi xác minh tên người dùng, email và câu trả lời bảo mật
    public boolean resetPassword(String username, String email, String answer1, String answer2, String newPassword) {
        if (!Validator.isValidUsername(username) || !Validator.isValidEmail(email) || !hasAnswer(answer1) ||
                !hasAnswer(answer2) || !Validator.isValidPassword(newPassword)) {
            return false;
        }

        username = Validator.normalizeAndLowercase(username);
        email = Validator.normalizeAndLowercase(email);
        answer1 = Validator.normalizeAndLowercase(answer1);
        answer2 = Validator.normalizeAndLowercase(answer2);
        newPassword = Validator.normalize(newPassword);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
            PreparedStatement selectStmt = conn.prepareStatement("""
                    SELECT * FROM users
                    WHERE username = ? AND email = ?
                    """)) {

            selectStmt.setString(1, username);
            selectStmt.setString(2, email);
            ResultSet rs = selectStmt.executeQuery();

            if (!rs.next()) {
                return false;
            }

            String currentHashedPassword = rs.getString("password");
            String passwordSalt = rs.getString("password_salt");
            String newHashedPassword = shared.utils.Hash.formula(newPassword, passwordSalt);

            if (currentHashedPassword.equals(newHashedPassword)) {
                System.err.println("Error resetting password: New password cannot be the same as the old password.");
                return false;
            }

            User user = mapUser(rs, conn);
            if (user == null || !user.resetPassword(answer1, answer2, newPassword)) {
                return false;
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE users SET password = ?, password_salt = ?, updated_at = CURRENT_TIMESTAMP WHERE username = ? AND email = ?")) {

                updateStmt.setString(1, user.getHashedPassword());
                updateStmt.setString(2, user.getPasswordSalt());
                updateStmt.setString(3, username);
                updateStmt.setString(4, email);

                int updatedRows = updateStmt.executeUpdate();
                if (updatedRows > 0) {
                    failedAttempts.remove(username);
                    lockUntil.remove(username);
                    loggedIn.remove(username);
                    return true;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error resetting password: " + e.getMessage());
        }

        return false;
    }

    // Đổi mật khẩu cho người dùng đã đăng nhập bằng mật khẩu hiện tại
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        if (!Validator.isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid username");
        }

        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Current password cannot be empty");
        }

        if (!Validator.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("New password must be at least 6 characters and include uppercase, lowercase, number, and special character");
        }

        username = Validator.normalizeAndLowercase(username);
        oldPassword = Validator.normalize(oldPassword);
        newPassword = Validator.normalize(newPassword);

        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement selectStmt = conn.prepareStatement("""
                                SELECT * FROM users
                                WHERE username = ?
                                """)) {

            selectStmt.setString(1, username);
            ResultSet rs = selectStmt.executeQuery();

            if (!rs.next()) {
                throw new IllegalArgumentException("User not found");
            }

            User user = mapUser(rs, conn);
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }

            if (!user.changePassword(oldPassword, newPassword)) {
                throw new IllegalArgumentException("Current password is incorrect or the new password is invalid");
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE users SET password = ?, password_salt = ?, updated_at = CURRENT_TIMESTAMP WHERE username = ?")) {

                updateStmt.setString(1, user.getHashedPassword());
                updateStmt.setString(2, user.getPasswordSalt());
                updateStmt.setString(3, username);

                int updatedRows = updateStmt.executeUpdate();
                if (updatedRows > 0) {
                    failedAttempts.remove(username);
                    lockUntil.remove(username);
                    return true;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
            throw new RuntimeException("Database error while changing password");
        }

        throw new IllegalStateException("Failed to update password");
    }

    private boolean hasAnswer(String answer) {
        return answer != null && !answer.trim().isEmpty();
    }

    private User mapUser(ResultSet rs, Connection conn) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String role = rs.getString("role");
        String email = rs.getString("email");
        String hashedPassword = rs.getString("password");
        String passwordSalt = rs.getString("password_salt");
        String q1 = rs.getString("question_1");
        String hashedA1 = rs.getString("answer_1");
        String saltA1 = rs.getString("answer_salt_1");
        String q2 = rs.getString("question_2");
        String hashedA2 = rs.getString("answer_2");
        String saltA2 = rs.getString("answer_salt_2");
        boolean isBanned = rs.getBoolean("is_banned");
        boolean isOnline = rs.getBoolean("is_online");

        User user = null;
        if ("BIDDER".equals(role)) {
            user = new Bidder(id, username, hashedPassword, passwordSalt, email, isBanned, q1, hashedA1, saltA1, q2,
                    hashedA2, saltA2);
        } else if ("SELLER".equals(role)) {
            user = new Seller(id, username, hashedPassword, passwordSalt, email, isBanned, q1, hashedA1, saltA1, q2,
                    hashedA2, saltA2);
        } else if ("ADMIN".equals(role)) {
            user = new Admin(id, username, hashedPassword, passwordSalt, email, isBanned, q1, hashedA1, saltA1, q2,
                    hashedA2, saltA2);
        }

        if (user != null) {
            user.setOnline(isOnline);
            if (isBanned) {
                user.banUser();
            }
        }

        loadWalletBalance(conn, user);
        return user;
    }

    // Logout người dùng
    public void logout(String username) {
        loggedIn.remove(username);
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt =
                     conn.prepareStatement(
                             "UPDATE users SET is_online = FALSE WHERE username = ?")) {

            pstmt.setString(1, username);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating online status: " + e.getMessage());
        }
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {

            while (rs.next()) {
                User user = mapUser(rs, conn);
                if (user != null) {
                    userList.add(user);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }

        return userList;
    }

    // Cấm người dùng - trả về null nếu thành công, thông báo lỗi nếu không thành công
    public String banUser(String username, String adminUsername) {
        username = Validator.normalizeAndLowercase(username);
        User admin = getUserFromDatabase(adminUsername);
        User targetUser = getUserFromDatabase(username);

        if (admin == null || targetUser == null) {
            return "User not found";
        }

        if (admin.getRole() != Role.ADMIN) {
            return "Only admin can ban users";
        }

        if (targetUser.getRole() == Role.ADMIN) {
            return "Cannot ban an administrator";
        }

        // Kiểm tra xem người dùng đã bị cấm chưa
        if (isBanned(username)) {
            return "User is already banned";
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn
                        .prepareStatement("UPDATE users SET is_banned = TRUE WHERE username = ?")) {

            pstmt.setString(1, username);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logAdminAction(admin.getId(), targetUser.getId(), "BAN");
                return null; // Success
            }

            return "Failed to ban user";
        } catch (SQLException e) {
            System.err.println("Error banning user: " + e.getMessage());
            return "Database error while banning user";
        }
    }

    // Bỏ cấm người dùng - trả về null nếu thành công, thông báo lỗi nếu không thành công
    public String unbanUser(String username, String adminUsername) {
        username = Validator.normalizeAndLowercase(username);
        User admin = getUserFromDatabase(adminUsername);
        User targetUser = getUserFromDatabase(username);

        if (admin == null || targetUser == null) {
            return "User not found";
        }

        if (admin.getRole() != Role.ADMIN) {
            return "Only admin can unban users";
        }

        if (targetUser.getRole() == Role.ADMIN) {
            return "Cannot unban an administrator";
        }

        // Kiểm tra xem người dùng đã bị cấm chưa
        if (!isBanned(username)) {
            return "User is not banned";
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn
                        .prepareStatement("UPDATE users SET is_banned = FALSE WHERE username = ?")) {

            pstmt.setString(1, username);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logAdminAction(admin.getId(), targetUser.getId(), "UNBAN");
                return null; // Thành công
            }

            return "Failed to unban user";
        } catch (SQLException e) {
            System.err.println("Error unbanning user: " + e.getMessage());
            return "Database error while unbanning user";
        }
    }

    private void logAdminAction(int adminId, int targetUserId, String action) {
        String sql = "INSERT INTO admin_action_logs (admin_id, target_user_id, action) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, adminId);
            pstmt.setInt(2, targetUserId);
            pstmt.setString(3, action);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging admin action: " + e.getMessage());
        }
    }

    public List<AdminActionLog> getAdminActionLogs() {
        List<AdminActionLog> logs = new ArrayList<>();
        String sql = "SELECT aal.id, u_admin.username AS admin_username, u_target.username AS target_username, aal.action, aal.action_time "
                + "FROM admin_action_logs aal " + "JOIN users u_admin ON aal.admin_id = u_admin.id " +
                "JOIN users u_target ON aal.target_user_id = u_target.id " + "ORDER BY aal.action_time DESC";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String adminUsername = rs.getString("admin_username");
                String targetUsername = rs.getString("target_username");
                String action = rs.getString("action");
                Instant actionTime = rs.getTimestamp("action_time").toInstant();
                logs.add(new AdminActionLog(id, adminUsername, targetUsername, action, actionTime));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching admin action logs: " + e.getMessage());
        }

        return logs;
    }

    private void ensureWalletExists(Connection conn, int userId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("""
                        INSERT INTO wallets (user_id, balance, currency, created_at, updated_at)
                        SELECT ?, 0, 'USD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                        WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE user_id = ?)
                        """)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    private BigDecimal getWalletBalance(Connection conn, int userId) throws SQLException {
        ensureWalletExists(conn, userId);
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT balance FROM wallets WHERE user_id = ? ORDER BY id ASC LIMIT 1")) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                BigDecimal balance = rs.getBigDecimal("balance");
                return balance == null ? BigDecimal.ZERO : balance;
            }

            return BigDecimal.ZERO;
        }
    }

    private void loadWalletBalance(Connection conn, User user) throws SQLException {
        if (user == null) {
            return;
        }

        BigDecimal balance = getWalletBalance(conn, user.getId());
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (user instanceof Bidder bidder) {
            bidder.getWallet().deposit(balance);
        } else if (user instanceof Seller seller) {
            seller.getWallet().deposit(balance);
        }
    }
}