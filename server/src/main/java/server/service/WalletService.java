package server.service;

import server.database.DatabaseConfig;
import shared.enums.RequestStatus;
import shared.utils.Validator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WalletService {
    public BigDecimal getWalletBalance(String username) {
        if (!Validator.isValidUsername(username)) {
            return null;
        }

        username = Validator.normalizeUsername(username);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            Integer userId = getUserIdByUsername(conn, username);
            if (userId == null) {
                return null;
            }

            ensureWalletExists(conn, userId);
            return getWalletBalance(conn, userId);
        } catch (SQLException e) {
            System.err.println("Error getting wallet balance: " + e.getMessage());
            return null;
        }
    }

    public String createDepositRequest(String bidderUsername, BigDecimal amount) {
        if (!Validator.isValidUsername(bidderUsername)) {
            return "Invalid username";
        }
        if (!isPositiveAmount(amount)) {
            return "Amount must be greater than 0";
        }

        bidderUsername = Validator.normalizeUsername(bidderUsername);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     """
                     INSERT INTO deposit_requests (id, bidder_id, amount, status, created_at, updated_at)
                     VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                     """)) {

            Integer bidderId = getUserIdByUsernameAndRole(conn, bidderUsername, "BIDDER");
            if (bidderId == null) {
                return "Bidder not found";
            }

            ensureWalletExists(conn, bidderId);

            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setInt(2, bidderId);
            pstmt.setBigDecimal(3, amount);
            pstmt.setString(4, RequestStatus.PENDING.name());
            pstmt.executeUpdate();
            return null;
        } catch (SQLException e) {
            System.err.println("Error creating deposit request: " + e.getMessage());
            return "Database error while creating deposit request";
        }
    }

    public String createWithdrawRequest(String sellerUsername, BigDecimal amount, String bankName, String accountNumber) {
        if (sellerUsername == null || sellerUsername.trim().isEmpty()) {
            return "Username is required";
        }
        if (!Validator.isValidUsername(sellerUsername)) {
            return "Invalid username";
        }
        if (amount == null) {
            return "Amount is required";
        }
        if (!isPositiveAmount(amount)) {
            return "Amount must be greater than 0";
        }
        if (bankName == null || bankName.trim().isEmpty()) {
            return "Bank name cannot be empty";
        }
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return "Bank account cannot be empty";
        }

        sellerUsername = Validator.normalizeUsername(sellerUsername);
        accountNumber = accountNumber.trim();

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     """
                     INSERT INTO withdraw_requests (id, seller_id, amount, bank_account, status, created_at, updated_at)
                     VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                     """)) {

            Integer sellerId = getUserIdByUsername(conn, sellerUsername);
            if (sellerId == null) {
                return "User not found";
            }

            ensureWalletExists(conn, sellerId);
            BigDecimal balance = getWalletBalance(conn, sellerId);
            if (balance.compareTo(amount) < 0) {
                return "Insufficient wallet balance";
            }

            pstmt.setString(1, UUID.randomUUID().toString());
            pstmt.setInt(2, sellerId);
            pstmt.setBigDecimal(3, amount);
            pstmt.setString(4, bankName.trim() + " - " + accountNumber);
            pstmt.setString(5, RequestStatus.PENDING.name());
            pstmt.executeUpdate();
            return null;
        } catch (SQLException e) {
            System.err.println("Error creating withdraw request: " + e.getMessage());
            return "Database error while creating withdraw request";
        }
    }

    public List<Map<String, String>> getPendingDepositRequests() {
        List<Map<String, String>> requests = new ArrayList<>();

        String sql = """
                SELECT dr.id, u.username, dr.amount, dr.status, dr.created_at
                FROM deposit_requests dr
                JOIN users u ON dr.bidder_id = u.id
                WHERE dr.status = ?
                ORDER BY dr.created_at ASC
                """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, RequestStatus.PENDING.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id", rs.getString("id"));
                row.put("username", rs.getString("username"));
                row.put("amount", rs.getBigDecimal("amount").toPlainString());
                row.put("status", rs.getString("status"));
                row.put("createdAt", rs.getTimestamp("created_at").toString());
                requests.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error loading deposit requests: " + e.getMessage());
        }

        return requests;
    }

    public List<Map<String, String>> getPendingWithdrawRequests() {
        List<Map<String, String>> requests = new ArrayList<>();

        String sql = """
                SELECT wr.id, u.username, wr.amount, wr.bank_account, wr.status, wr.created_at
                FROM withdraw_requests wr
                JOIN users u ON wr.seller_id = u.id
                WHERE wr.status = ?
                ORDER BY wr.created_at ASC
                """;

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, RequestStatus.PENDING.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                row.put("id", rs.getString("id"));
                row.put("username", rs.getString("username"));
                row.put("amount", rs.getBigDecimal("amount").toPlainString());
                row.put("bankAccount", rs.getString("bank_account"));
                row.put("status", rs.getString("status"));
                row.put("createdAt", rs.getTimestamp("created_at").toString());
                requests.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error loading withdraw requests: " + e.getMessage());
        }

        return requests;
    }

    public String approveDeposit(String requestId, String adminUsername) {
        return processDepositDecision(requestId, adminUsername, RequestStatus.APPROVED);
    }

    public String rejectDeposit(String requestId, String adminUsername) {
        return processDepositDecision(requestId, adminUsername, RequestStatus.REJECTED);
    }

    public String approveWithdraw(String requestId, String adminUsername) {
        return processWithdrawDecision(requestId, adminUsername, RequestStatus.APPROVED);
    }

    public String rejectWithdraw(String requestId, String adminUsername) {
        return processWithdrawDecision(requestId, adminUsername, RequestStatus.REJECTED);
    }

    private String processDepositDecision(String requestId, String adminUsername, RequestStatus status) {
        if (!isValidRequestInput(requestId, adminUsername)) {
            return "Invalid request";
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (getUserIdByUsernameAndRole(conn, Validator.normalizeUsername(adminUsername), "ADMIN") == null) {
                    conn.rollback();
                    return "Only admin can process deposit requests";
                }

                PendingRequestData requestData = getPendingDepositRequest(conn, requestId);
                if (requestData == null) {
                    conn.rollback();
                    return "Deposit request not found or already processed";
                }

                if (status == RequestStatus.APPROVED) {
                    ensureWalletExists(conn, requestData.userId());
                    updateWalletBalance(conn, requestData.userId(), requestData.amount());
                }

                updateDepositRequestStatus(conn, requestId, status);
                conn.commit();
                return null;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error processing deposit request: " + e.getMessage());
                return "Database error while processing deposit request";
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error processing deposit request: " + e.getMessage());
            return "Database error while processing deposit request";
        }
    }

    private String processWithdrawDecision(String requestId, String adminUsername, RequestStatus status) {
        if (!isValidRequestInput(requestId, adminUsername)) {
            return "Invalid request";
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (getUserIdByUsernameAndRole(conn, Validator.normalizeUsername(adminUsername), "ADMIN") == null) {
                    conn.rollback();
                    return "Only admin can process withdraw requests";
                }

                PendingRequestData requestData = getPendingWithdrawRequest(conn, requestId);
                if (requestData == null) {
                    conn.rollback();
                    return "Withdraw request not found or already processed";
                }

                if (status == RequestStatus.APPROVED) {
                    ensureWalletExists(conn, requestData.userId());
                    BigDecimal currentBalance = getWalletBalance(conn, requestData.userId());
                    if (currentBalance.compareTo(requestData.amount()) < 0) {
                        conn.rollback();
                        return "Seller wallet balance is no longer sufficient";
                    }
                    updateWalletBalance(conn, requestData.userId(), requestData.amount().negate());
                }

                updateWithdrawRequestStatus(conn, requestId, status);
                conn.commit();
                return null;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error processing withdraw request: " + e.getMessage());
                return "Database error while processing withdraw request";
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error processing withdraw request: " + e.getMessage());
            return "Database error while processing withdraw request";
        }
    }

    private boolean isValidRequestInput(String requestId, String adminUsername) {
        return requestId != null && !requestId.trim().isEmpty() && Validator.isValidUsername(adminUsername);
    }

    private boolean isPositiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private Integer getUserIdByUsername(Connection conn, String username) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return null;
        }
    }

    private Integer getUserIdByUsernameAndRole(Connection conn, String username, String role) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT id FROM users WHERE username = ? AND role = ? AND is_banned = FALSE")) {
            pstmt.setString(1, username);
            pstmt.setString(2, role);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return null;
        }
    }

    private void ensureWalletExists(Connection conn, int userId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT 1 FROM wallets WHERE user_id = ?")) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                try (PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO wallets (user_id, balance, currency, created_at, updated_at) VALUES (?, 0, 'USD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
                    insertStmt.setInt(1, userId);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    private BigDecimal getWalletBalance(Connection conn, int userId) throws SQLException {
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

    private void updateWalletBalance(Connection conn, int userId, BigDecimal amountDelta) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                """
                UPDATE wallets
                SET balance = balance + ?, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                """)) {
            pstmt.setBigDecimal(1, amountDelta);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    private PendingRequestData getPendingDepositRequest(Connection conn, String requestId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT bidder_id, amount FROM deposit_requests WHERE id = ? AND status = ?")) {
            pstmt.setString(1, requestId);
            pstmt.setString(2, RequestStatus.PENDING.name());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new PendingRequestData(rs.getInt("bidder_id"), rs.getBigDecimal("amount"));
            }
            return null;
        }
    }

    private PendingRequestData getPendingWithdrawRequest(Connection conn, String requestId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT seller_id, amount FROM withdraw_requests WHERE id = ? AND status = ?")) {
            pstmt.setString(1, requestId);
            pstmt.setString(2, RequestStatus.PENDING.name());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new PendingRequestData(rs.getInt("seller_id"), rs.getBigDecimal("amount"));
            }
            return null;
        }
    }

    private void updateDepositRequestStatus(Connection conn, String requestId, RequestStatus status) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE deposit_requests SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, requestId);
            pstmt.executeUpdate();
        }
    }

    private void updateWithdrawRequestStatus(Connection conn, String requestId, RequestStatus status) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "UPDATE withdraw_requests SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, requestId);
            pstmt.executeUpdate();
        }
    }

    private record PendingRequestData(int userId, BigDecimal amount) {
    }

    /* --- Hold / reservation functionality for bids --- */

    /**
     * Get available wallet balance (just total balance, no holds)
     */
    public BigDecimal getAvailableBalance(String username) {
        if (!Validator.isValidUsername(username)) return null;
        username = Validator.normalizeUsername(username);
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            Integer userId = getUserIdByUsername(conn, username);
            if (userId == null) return null;
            ensureWalletExists(conn, userId);
            return getWalletBalance(conn, userId);
        } catch (SQLException e) {
            System.err.println("Error getting available balance: " + e.getMessage());
            return null;
        }
    }

    private BigDecimal getTotalHeldAmount(Connection conn, int userId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT COALESCE(SUM(amount),0) as total FROM wallet_holds WHERE bidder_id = ? AND status = 'HELD'")) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
            return BigDecimal.ZERO;
        }
    }

    /**
     * Create or update a hold for a bidder on an auction. Returns null on success, or error message.
     */
    public String createOrUpdateHold(String auctionId, String bidderUsername, BigDecimal amount) {
        if (auctionId == null || auctionId.isBlank()) return "Invalid auction id";
        if (!Validator.isValidUsername(bidderUsername)) return "Invalid bidder";
        if (!isPositiveAmount(amount)) return "Amount must be positive";
        bidderUsername = Validator.normalizeUsername(bidderUsername);
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer bidderId = getUserIdByUsernameAndRole(conn, bidderUsername, "BIDDER");
                if (bidderId == null) {
                    conn.rollback();
                    return "Bidder not found";
                }
                ensureWalletExists(conn, bidderId);
                // If there's an existing hold for this auction by the bidder, allow updating it by
                // considering the existing hold amount when checking available funds.
                BigDecimal existingHold = BigDecimal.ZERO;
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT amount FROM wallet_holds WHERE auction_id = ? AND bidder_id = ? AND status = 'HELD'")) {
                    check.setString(1, auctionId);
                    check.setInt(2, bidderId);
                    ResultSet rs = check.executeQuery();
                    if (rs.next()) {
                        existingHold = rs.getBigDecimal("amount");
                        if (existingHold == null) existingHold = BigDecimal.ZERO;
                    }
                }

                BigDecimal totalHeld = getTotalHeldAmount(conn, bidderId);
                if (totalHeld == null) totalHeld = BigDecimal.ZERO;
                BigDecimal totalHeldExcludingCurrent = totalHeld.subtract(existingHold == null ? BigDecimal.ZERO : existingHold);
                BigDecimal balance = getWalletBalance(conn, bidderId);
                BigDecimal available = balance.subtract(totalHeldExcludingCurrent);
                // Now ensure available (excluding current auction's existing hold) is enough for the requested amount
                if (available.compareTo(amount) < 0) {
                    conn.rollback();
                    return "Insufficient available balance";
                }

                // Upsert hold
                try (PreparedStatement upsert = conn.prepareStatement(
                        "INSERT INTO wallet_holds (auction_id, bidder_id, amount, status, created_at, updated_at) " +
                                "VALUES (?, ?, ?, 'HELD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                                "ON CONFLICT(auction_id, bidder_id) DO UPDATE SET amount = EXCLUDED.amount, status = 'HELD', updated_at = CURRENT_TIMESTAMP")) {
                    upsert.setString(1, auctionId);
                    upsert.setInt(2, bidderId);
                    upsert.setBigDecimal(3, amount);
                    upsert.executeUpdate();
                }

                conn.commit();
                return null;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error creating/updating hold: " + e.getMessage());
                return "Database error while creating hold";
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error creating/updating hold: " + e.getMessage());
            return "Database error while creating hold";
        }
    }

    /**
     * Release (cancel) a hold for a bidder on an auction
     */
    public void releaseHold(String auctionId, String bidderUsername) {
        if (auctionId == null || auctionId.isBlank() || !Validator.isValidUsername(bidderUsername)) return;
        bidderUsername = Validator.normalizeUsername(bidderUsername);
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            Integer bidderId = getUserIdByUsername(conn, bidderUsername);
            if (bidderId == null) return;
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE wallet_holds SET status = 'RELEASED', updated_at = CURRENT_TIMESTAMP WHERE auction_id = ? AND bidder_id = ? AND status = 'HELD'")) {
                pstmt.setString(1, auctionId);
                pstmt.setInt(2, bidderId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error releasing hold: " + e.getMessage());
        }
    }

    /**
     * Release all holds for an auction (used when auction finishes to release non-winning holds)
     */
    public void releaseAllHoldsForAuction(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) return;
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE wallet_holds SET status = 'RELEASED', updated_at = CURRENT_TIMESTAMP WHERE auction_id = ? AND status = 'HELD'")) {
            pstmt.setString(1, auctionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error releasing holds for auction: " + e.getMessage());
        }
    }

    /**
     * Finalize payment for a winning bidder: deduct amount and credit seller.
     * Returns null on success or error message.
     */
    public String finalizePaymentForWinner(String auctionId, String bidderUsername, String sellerUsername, BigDecimal amount) {
        if (auctionId == null || auctionId.isBlank()) return "Invalid auction id";
        if (!Validator.isValidUsername(bidderUsername) || !Validator.isValidUsername(sellerUsername)) return "Invalid user";
        if (!isPositiveAmount(amount)) return "Invalid amount";

        bidderUsername = Validator.normalizeUsername(bidderUsername);
        sellerUsername = Validator.normalizeUsername(sellerUsername);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer bidderId = getUserIdByUsername(conn, bidderUsername);
                Integer sellerId = getUserIdByUsername(conn, sellerUsername);
                if (bidderId == null || sellerId == null) {
                    conn.rollback();
                    return "User not found";
                }
                ensureWalletExists(conn, bidderId);
                ensureWalletExists(conn, sellerId);

                // Check bidder has sufficient balance to pay
                BigDecimal bidderBalance = getWalletBalance(conn, bidderId);
                if (bidderBalance.compareTo(amount) < 0) {
                    conn.rollback();
                    return "Bidder insufficient balance";
                }

                // Deduct amount from bidder wallet
                updateWalletBalance(conn, bidderId, amount.negate());
                // Credit seller wallet
                updateWalletBalance(conn, sellerId, amount);


                conn.commit();
                return null;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error finalizing payment: " + e.getMessage());
                return "Database error while finalizing payment";
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error finalizing payment: " + e.getMessage());
            return "Database error while finalizing payment";
        }
    }
}
