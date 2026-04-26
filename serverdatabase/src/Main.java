import server.database.DatabaseConfig;
import shared.enums.Role;
import shared.utils.Validator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        // Register bidder2 directly into database
        String username = "bidder2";
        String password = "Admin@123"; // Same as default
        String email = "bidder2@gmail.com";
        String q1 = "q";
        String a1 = "a";
        String q2 = "q";
        String a2 = "a";
        Role role = Role.BIDDER;

        if (!Validator.isValidUsername(username)) {
            System.out.println("Invalid username");
            return;
        }
        if (!Validator.isValidPassword(password)) {
            System.out.println("Invalid password");
            return;
        }
        if (!Validator.isValidEmail(email)) {
            System.out.println("Invalid email");
            return;
        }

        username = Validator.normalizeAndLowercase(username);
        password = Validator.normalize(password);
        email = Validator.normalizeAndLowercase(email);
        q1 = Validator.normalize(q1);
        q2 = Validator.normalize(q2);
        a1 = Validator.normalizeAndLowercase(a1);
        a2 = Validator.normalizeAndLowercase(a2);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, email, role, question_1, answer_1, question_2, answer_2) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {

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
                System.out.println("User registered successfully!");
                var generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);
                    // Ensure wallet
                    ensureWalletExists(conn, userId);
                }
            } else {
                System.out.println("Failed to register user");
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate key")) {
                System.out.println("Username already exists");
            } else {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private static void ensureWalletExists(Connection conn, int userId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                """
                INSERT INTO wallets (user_id, balance, currency, created_at, updated_at)
                SELECT ?, 0, 'USD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                WHERE NOT EXISTS (SELECT 1 FROM wallets WHERE user_id = ?)
                """)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            System.out.println("Wallet created for user");
        }
    }
}
