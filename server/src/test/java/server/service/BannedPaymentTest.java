package server.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.database.DatabaseConfig;
import shared.enums.Role;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class BannedPaymentTest {
    private UserService userService;
    private WalletService walletService;
    private String testBidder;
    private String testSeller;

    @BeforeEach
    public void setUp() {
        userService = new UserService();
        walletService = new WalletService();
        userService.initializeDefaultUsers();
    }

    @AfterEach
    public void tearDown() {
        try (java.sql.Connection conn = DatabaseConfig.getDataSource().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE username IN (?, ?)")) {
            stmt.setString(1, testBidder);
            stmt.setString(2, testSeller);
            stmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBannedUserCannotBeCharged() {
        testBidder = "bidder_" + UUID.randomUUID().toString().substring(0, 8);
        testSeller = "seller_" + UUID.randomUUID().toString().substring(0, 8);
        String admin = "admin";

        // 1. Register users
        userService.register(testBidder, "Pass@123", testBidder + "@test.com", "q", "a", "q", "a", Role.BIDDER);
        userService.register(testSeller, "Pass@123", testSeller + "@test.com", "q", "a", "q", "a", Role.SELLER);

        // 2. Deposit money into the bidder's wallet
        String depositResult = walletService.createDepositRequest(testBidder, new BigDecimal("1000"), "Test Bank", "12345");

        assertNull(depositResult);
        // Get the ID of the request just created to approve it
        String requestId = walletService.getPendingDepositRequests().stream()
                .filter(r -> r.get("username").equals(testBidder))
                .findFirst()
                .map(r -> r.get("id"))
                .orElseThrow(() -> new RuntimeException("Deposit request not found"));
        walletService.approveDeposit(requestId, admin);

        // 3. Ban the bidder
        userService.banUser(testBidder, admin);
        assertTrue(userService.isUserBanned(testBidder), "User should be banned");

        // 4. Finalize payment for an auction where the banned bidder won
        String result = walletService.finalizePaymentForWinner(UUID.randomUUID().toString(), testBidder, testSeller,
                new BigDecimal("100.00"));

        // 5. Check that the payment was blocked due to the ban
        assertEquals("Bidder is banned. Payment blocked.", result);
    }
}