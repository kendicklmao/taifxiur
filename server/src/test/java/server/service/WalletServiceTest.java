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

    private String testAdmin;
    private String testBidder;
    private String testSeller;

    @BeforeAll
    public static void setUpClass() throws Exception {
        DatabaseInitializer.initializeDatabase();
        userService = new UserService();
        walletService = new WalletService();
        }

    @BeforeEach
    public void setUp() {
        // Dọn dẹp DB trước khi chạy để đảm bảo môi trường sạch
        cleanupDatabase();
        // Khởi tạo người dùng riêng cho mỗi test để không ảnh hưởng DB thật
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        testAdmin = "admin_" + suffix;
        testBidder = "bidder_" + suffix;
        testSeller = "seller_" + suffix;

        userService.register(testAdmin, "Password@123", "admin" + suffix + "@test.com", "q1", "a1", "q2", "a2", shared.enums.Role.ADMIN);
        userService.register(testBidder, "Password@123", "bidder" + suffix + "@test.com", "q1", "a1", "q2", "a2", shared.enums.Role.BIDDER);
        userService.register(testSeller, "Password@123", "seller" + suffix + "@test.com", "q1", "a1", "q2", "a2", shared.enums.Role.SELLER);
    }

    @AfterEach // Chạy sau mỗi test
    public void tearDown() {
        // Dọn dẹp DB bằng cách xóa các user vừa tạo (Cascade sẽ xóa các request, wallet liên quan)
        try (java.sql.Connection conn = DatabaseConfig.getDataSource().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE username IN (?, ?, ?)")) {
            stmt.setString(1, testAdmin);
            stmt.setString(2, testBidder);
            stmt.setString(3, testSeller);
            stmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    @AfterAll // Chạy một lần sau tất cả các test
    public static void tearDownClass() {
        DatabaseConfig.closeDataSource();
    }

    // Phương thức dọn dẹp database cũ đã bị bỏ vì ta không muốn xóa dữ liệu thật
    private void cleanupDatabase() {
        // FIXME: Commented out to prevent wiping the actual database during tests
    }

    @Test
    public void testCreateDepositRequest() {
        String bankAccount = java.util.UUID.randomUUID().toString().substring(0, 10);
        String result = walletService.createDepositRequest(testBidder, new BigDecimal("100.00"), "Test Bank", bankAccount);
        assertNull(result);

        List<Map<String, String>> requests = walletService.getPendingDepositRequests();
        boolean found = requests.stream().anyMatch(r -> bankAccount.equals(r.get("accountNumber")));
        assertTrue(found, "Deposit request should be in pending list");
    }

    @Test
    public void testApproveDepositRequest() {
        String bankAccount = java.util.UUID.randomUUID().toString().substring(0, 10);
        walletService.createDepositRequest(testBidder, new BigDecimal("100.00"), "Test Bank", bankAccount);
        List<Map<String, String>> requests = walletService.getPendingDepositRequests();
        
        String requestId = requests.stream()
                .filter(r -> bankAccount.equals(r.get("accountNumber")))
                .findFirst()
                .map(r -> r.get("id"))
                .orElse(null);
        assertNotNull(requestId, "Request ID should not be null");

        String result = walletService.approveDeposit(requestId, testAdmin);
        assertNull(result, "Approval should succeed");

        BigDecimal balance = walletService.getWalletBalance(testBidder);
        assertNotNull(balance, "Bidder balance should not be null");
        assertEquals(0, balance.compareTo(new BigDecimal("100.00")), "Balance should be 100.00");
    }

    @Test
    public void testRejectDepositRequest() {
        String bankAccount = java.util.UUID.randomUUID().toString().substring(0, 10);
        walletService.createDepositRequest(testBidder, new BigDecimal("100.00"), "Test Bank", bankAccount);
        List<Map<String, String>> requests = walletService.getPendingDepositRequests();

        String requestId = requests.stream()
                .filter(r -> bankAccount.equals(r.get("accountNumber")))
                .findFirst()
                .map(r -> r.get("id"))
                .orElse(null);
        assertNotNull(requestId, "Request ID should not be null");

        String result = walletService.rejectDeposit(requestId, testAdmin);
        assertNull(result, "Rejection should succeed");

        BigDecimal balance = walletService.getWalletBalance(testBidder);
        assertNotNull(balance, "Bidder balance should not be null");
        assertEquals(0, balance.compareTo(BigDecimal.ZERO), "Balance should remain 0");
    }

    @Test
    public void testCreateWithdrawRequest() {
        // First deposit to have balance
        String depAccount = java.util.UUID.randomUUID().toString().substring(0, 10);
        walletService.createDepositRequest(testSeller, new BigDecimal("200.00"), "Test Bank", depAccount);
        String depRequestId = walletService.getPendingDepositRequests().stream()
                .filter(r -> depAccount.equals(r.get("accountNumber")))
                .findFirst().map(r -> r.get("id")).orElse(null);
        walletService.approveDeposit(depRequestId, testAdmin);

        String withAccount = java.util.UUID.randomUUID().toString().substring(0, 10);
        String result = walletService.createWithdrawRequest(testSeller, new BigDecimal("50.00"), "Bank", withAccount);
        assertNull(result);

        List<Map<String, String>> withdraws = walletService.getPendingWithdrawRequests();
        boolean found = withdraws.stream().anyMatch(r -> withAccount.equals(r.get("accountNumber")));
        assertTrue(found, "Withdraw request should be in pending list");
    }

    @Test
    public void testApproveWithdrawRequest() {
        String depAccount = java.util.UUID.randomUUID().toString().substring(0, 10);
        walletService.createDepositRequest(testSeller, new BigDecimal("200.00"), "Test Bank", depAccount);
        String depRequestId = walletService.getPendingDepositRequests().stream()
                .filter(r -> depAccount.equals(r.get("accountNumber")))
                .findFirst().map(r -> r.get("id")).orElse(null);
        walletService.approveDeposit(depRequestId, testAdmin);

        String withAccount = java.util.UUID.randomUUID().toString().substring(0, 10);
        walletService.createWithdrawRequest(testSeller, new BigDecimal("50.00"), "Bank", withAccount);
        List<Map<String, String>> withdraws = walletService.getPendingWithdrawRequests();

        String requestId = withdraws.stream()
                .filter(r -> withAccount.equals(r.get("accountNumber")))
                .findFirst()
                .map(r -> r.get("id"))
                .orElse(null);
        assertNotNull(requestId, "Request ID should not be null");

        String result = walletService.approveWithdraw(requestId, testAdmin);
        assertNull(result, "Approval should succeed");

        BigDecimal balance = walletService.getWalletBalance(testSeller);
        assertNotNull(balance, "Seller balance should not be null");
        assertEquals(0, balance.compareTo(new BigDecimal("150.00")), "Balance should be 150.00");
    }

    @Test
    public void testRejectWithdrawRequest() {
        String depAccount = java.util.UUID.randomUUID().toString().substring(0, 10);
        walletService.createDepositRequest(testSeller, new BigDecimal("200.00"), "Test Bank", depAccount);
        String depRequestId = walletService.getPendingDepositRequests().stream()
                .filter(r -> depAccount.equals(r.get("accountNumber")))
                .findFirst().map(r -> r.get("id")).orElse(null);
        walletService.approveDeposit(depRequestId, testAdmin);

        String withAccount = java.util.UUID.randomUUID().toString().substring(0, 10);
        walletService.createWithdrawRequest(testSeller, new BigDecimal("50.00"), "Bank", withAccount);
        List<Map<String, String>> withdraws = walletService.getPendingWithdrawRequests();

        String requestId = withdraws.stream()
                .filter(r -> withAccount.equals(r.get("accountNumber")))
                .findFirst()
                .map(r -> r.get("id"))
                .orElse(null);
        assertNotNull(requestId, "Request ID should not be null");

        String result = walletService.rejectWithdraw(requestId, testAdmin);
        assertNull(result, "Rejection should succeed");

        BigDecimal balance = walletService.getWalletBalance(testSeller);
        assertNotNull(balance, "Seller balance should not be null");
        assertEquals(0, balance.compareTo(new BigDecimal("200.00")), "Balance should be 200.00");
    }
}
