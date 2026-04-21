package server.service;

import server.database.DatabaseConfig;
import shared.enums.RequestStatus;
import shared.models.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WalletService {
    private final ConcurrentHashMap<String, DepositRequest> depositRequests = new ConcurrentHashMap<>();//danh sách yêu cầu nạp tiền
    private final ConcurrentHashMap<String, WithdrawRequest> withdrawRequests = new ConcurrentHashMap<>();//danh sách yêu cầu rút tiền

    /**
     * Create a deposit request and store in database
     */
    public void createDepositRequest(Bidder bidder, BigDecimal amount) {
        if (bidder == null) {
            throw new IllegalArgumentException();
        }
        DepositRequest req = new DepositRequest(bidder, amount);
        depositRequests.put(req.getId(), req);
        
        // Store in database
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO deposit_requests (id, bidder_id, amount, status, created_at) VALUES (?, ?, ?, ?, ?)")) {
            
            pstmt.setString(1, req.getId());
            pstmt.setInt(2, getUserIdFromDatabase(bidder.getUsername()));
            pstmt.setBigDecimal(3, amount);
            pstmt.setString(4, RequestStatus.PENDING.name());
            pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating deposit request: " + e.getMessage());
        }
    }

    /**
     * Get pending deposit requests from database
     */
    public List<DepositRequest> getPendingDepositRequests() {
        List<DepositRequest> listDepositRequest = new ArrayList<>();
        for (DepositRequest r : depositRequests.values()) {
            if (r.getStatus() == RequestStatus.PENDING) {
                listDepositRequest.add(r);
            }
        }
        return listDepositRequest;
    }

    /**
     * Get pending withdraw requests from database
     */
    public List<WithdrawRequest> getPendingWithdrawRequests() {
        List<WithdrawRequest> listWithdrawRequest = new ArrayList<>();
        for (WithdrawRequest r : withdrawRequests.values()) {
            if (r.getStatus() == RequestStatus.PENDING) {
                listWithdrawRequest.add(r);
            }
        }
        return listWithdrawRequest;
    }

    /**
     * Approve deposit request and update database
     */
    public void approveDeposit(String requestId, Admin admin) {
        if (admin == null || requestId == null) {
            throw new IllegalArgumentException();
        }
        DepositRequest req = depositRequests.get(requestId);
        if (req == null || req.getStatus() != RequestStatus.PENDING) 
            throw new IllegalArgumentException();
        req.approveDeposit();
        req.getBidder().getWallet().deposit(req.getAmount());
        
        // Update database
        updateDepositRequestStatus(requestId, RequestStatus.APPROVED);
        updateUserWalletBalance(req.getBidder().getUsername(), req.getAmount());
    }

    /**
     * Reject deposit request and update database
     */
    public void rejectDeposit(String requestId, Admin admin) {
        if (admin == null || requestId == null) {
            throw new IllegalArgumentException();
        }
        DepositRequest req = depositRequests.get(requestId);
        if (req == null || req.getStatus() != RequestStatus.PENDING) 
            throw new IllegalArgumentException();
        req.rejectDeposit();
        
        // Update database
        updateDepositRequestStatus(requestId, RequestStatus.REJECTED);
    }

    /**
     * Approve withdraw request and update database
     */
    public void approveWithdraw(String requestId, Admin admin) {
        if (admin == null || requestId == null) {
            throw new IllegalArgumentException();
        }
        WithdrawRequest req = withdrawRequests.get(requestId);
        if (req == null || req.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException();
        }
        req.approveWithdraw();
        
        // Update database
        updateWithdrawRequestStatus(requestId, RequestStatus.APPROVED);
    }

    /**
     * Reject withdraw request and update database
     */
    public void rejectWithdraw(String requestId, Admin admin) {
        if (admin == null || requestId == null) {
            throw new IllegalArgumentException();
        }
        WithdrawRequest req = withdrawRequests.get(requestId);
        if (req == null || req.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException();
        }
        req.rejectWithdraw();
        
        // Update database
        updateWithdrawRequestStatus(requestId, RequestStatus.REJECTED);
    }

    /**
     * Create a withdraw request and store in database
     */
    public void createWithdrawRequest(Seller seller, BigDecimal amount, BankInfo bankInfo) {
        if (seller == null) {
            throw new IllegalArgumentException();
        }
        WithdrawRequest req = new WithdrawRequest(seller, amount, bankInfo);
        withdrawRequests.put(req.getId(), req);
        
        // Store in database
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO withdraw_requests (id, seller_id, amount, bank_account, status, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            
            pstmt.setString(1, req.getId());
            pstmt.setInt(2, getUserIdFromDatabase(seller.getUsername()));
            pstmt.setBigDecimal(3, amount);
            // store the bank account number in the bank_account column
            pstmt.setString(4, bankInfo.getAccountNumber());
            pstmt.setString(5, RequestStatus.PENDING.name());
            pstmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating withdraw request: " + e.getMessage());
        }
    }

    /**
     * Helper method to get user ID from database by username
     */
    private int getUserIdFromDatabase(String username) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user ID: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Update deposit request status in database
     */
    private void updateDepositRequestStatus(String requestId, RequestStatus status) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE deposit_requests SET status = ? WHERE id = ?")) {
            
            pstmt.setString(1, status.name());
            pstmt.setString(2, requestId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating deposit request status: " + e.getMessage());
        }
    }

    /**
     * Update withdraw request status in database
     */
    private void updateWithdrawRequestStatus(String requestId, RequestStatus status) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE withdraw_requests SET status = ? WHERE id = ?")) {
            
            pstmt.setString(1, status.name());
            pstmt.setString(2, requestId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating withdraw request status: " + e.getMessage());
        }
    }

    /**
     * Update user wallet balance in database
     */
    private void updateUserWalletBalance(String username, BigDecimal amount) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE wallets SET balance = balance + ? WHERE user_id = (SELECT id FROM users WHERE username = ?)")) {
            
            pstmt.setBigDecimal(1, amount);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating wallet balance: " + e.getMessage());
        }
    }
}