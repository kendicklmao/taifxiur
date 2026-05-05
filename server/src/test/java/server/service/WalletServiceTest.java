package server.service;

import org.junit.jupiter.api.*;
import server.database.DatabaseConfig;
import server.database.DatabaseInitializer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WalletServiceTest {

    private static WalletService walletService;
    private static UserService userService;

    @BeforeAll // Chạy một lần trước tất cả các test trong class này
    public static void setUpClass() throws Exception {
        DatabaseInitializer.initializeDatabase();
        userService = new UserService();
        walletService = new WalletService(userService);
        userService.setWalletService(walletService);
    }

    @BeforeEach // Chạy trước mỗi test
    public void setUp() {
        // Dọn dẹp DB trước khi chạy để đảm bảo môi trường sạch
        cleanupDatabase();
        // Khởi tạo lại người dùng mặc định cho mỗi test
        userService.initializeDefaultUsers();
    }

    @AfterEach // Chạy sau mỗi test
    public void tearDown() {
        cleanupDatabase(); // Dọn dẹp DB sau mỗi test
    }

    @AfterAll // Chạy một lần sau tất cả các test
    public static void tearDownClass() {
        DatabaseConfig.closeDataSource();
    }

    // Phương thức dọn dẹp database
    private void cleanupDatabase() {
        try (java.sql.Connection conn = DatabaseConfig.getDataSource().getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            // Xóa dữ liệu từ các bảng liên quan
            // Thứ tự xóa quan trọng nếu có ràng buộc khóa ngoại
            stmt.executeUpdate("DELETE FROM deposit_requests");
            stmt.executeUpdate("DELETE FROM withdraw_requests");
            stmt.executeUpdate("UPDATE wallets SET balance = 0");
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateDepositRequest() {
        String result = walletService.createDepositRequest("bidder", new BigDecimal("100.00"), "Test Bank", "12345");
        assertNull(result);

        List<Map<String, String>> requests = walletService.getPendingDepositRequests();
        assertFalse(requests.isEmpty());
    }

    @Test
    public void testApproveDepositRequest() {
        walletService.createDepositRequest("bidder", new BigDecimal("100.00"), "Test Bank", "12345");
        List<Map<String, String>> requests = walletService.getPendingDepositRequests();
        String requestId = requests.get(0).get("id");

        String result = walletService.approveDeposit(requestId, "admin");
        assertNull(result);

        BigDecimal balance = walletService.getWalletBalance("bidder");
        assertEquals(0, balance.compareTo(new BigDecimal("100.00")));
    }

    @Test
    public void testRejectDepositRequest() {
        walletService.createDepositRequest("bidder", new BigDecimal("100.00"), "Test Bank", "12345");
        List<Map<String, String>> requests = walletService.getPendingDepositRequests();
        String requestId = requests.get(0).get("id");

        String result = walletService.rejectDeposit(requestId, "admin");
        assertNull(result);

        BigDecimal balance = walletService.getWalletBalance("bidder");
        assertEquals(0, balance.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testCreateWithdrawRequest() {
        walletService.createDepositRequest("seller", new BigDecimal("200.00"), "Test Bank", "12345");
        List<Map<String, String>> deposits = walletService.getPendingDepositRequests();
        walletService.approveDeposit(deposits.get(0).get("id"), "admin");

        String result = walletService.createWithdrawRequest("seller", new BigDecimal("50.00"), "Bank", "12345");
        assertNull(result);

        List<Map<String, String>> withdraws = walletService.getPendingWithdrawRequests();
        assertFalse(withdraws.isEmpty());
    }

    @Test
    public void testApproveWithdrawRequest() {
        walletService.createDepositRequest("seller", new BigDecimal("200.00"), "Test Bank", "12345");
        List<Map<String, String>> deposits = walletService.getPendingDepositRequests();
        walletService.approveDeposit(deposits.get(0).get("id"), "admin");

        walletService.createWithdrawRequest("seller", new BigDecimal("50.00"), "Bank", "12345");
        List<Map<String, String>> withdraws = walletService.getPendingWithdrawRequests();
        String requestId = withdraws.get(0).get("id");

        String result = walletService.approveWithdraw(requestId, "admin");
        assertNull(result);

        BigDecimal balance = walletService.getWalletBalance("seller");
        assertEquals(0, balance.compareTo(new BigDecimal("150.00")));
    }

    @Test
    public void testRejectWithdrawRequest() {
        walletService.createDepositRequest("seller", new BigDecimal("200.00"), "Test Bank", "12345");
        List<Map<String, String>> deposits = walletService.getPendingDepositRequests();
        walletService.approveDeposit(deposits.get(0).get("id"), "admin");

        walletService.createWithdrawRequest("seller", new BigDecimal("50.00"), "Bank", "12345");
        List<Map<String, String>> withdraws = walletService.getPendingWithdrawRequests();
        String requestId = withdraws.get(0).get("id");

        String result = walletService.rejectWithdraw(requestId, "admin");
        assertNull(result);

        BigDecimal balance = walletService.getWalletBalance("seller");
        assertEquals(0, balance.compareTo(new BigDecimal("200.00")));
    }
}
