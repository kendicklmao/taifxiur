package server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import server.database.DatabaseConfig;
import shared.enums.Role;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class BannedPaymentTest {
    private UserService userService;
    private WalletService walletService;

    @BeforeEach
    public void setUp() {
        userService = new UserService();
        walletService = new WalletService();
        userService.setWalletService(walletService);
        userService.initializeDefaultUsers();
    }

    @AfterEach
    public void tearDown() {
        DatabaseConfig.closeDataSource();
    }

    @Test
    public void testBannedUserCannotBeCharged() {
        String bidder = "bidder_" + UUID.randomUUID().toString().substring(0, 8);
        String seller = "seller_" + UUID.randomUUID().toString().substring(0, 8);
        String admin = "admin";

        // 1. Register users
        userService.register(bidder, "Pass@123", bidder + "@test.com", "q", "a", "q", "a", Role.BIDDER);
        userService.register(seller, "Pass@123", seller + "@test.com", "q", "a", "q", "a", Role.SELLER);

        // 2. Deposit money into the bidder's wallet
        walletService.createDepositRequest(bidder, new BigDecimal("1000"), "Test Bank", "12345");

        // Get the ID of the request just created to approve it
        String requestId = walletService.getPendingDepositRequests().stream()
                .filter(r -> r.get("username").equals(bidder))
                .findFirst()
                .get()
                .get("id");
        walletService.approveDeposit(requestId, admin);

        // 3. Ban the bidder
        userService.banUser(bidder, admin);
        assertTrue(userService.isUserBanned(bidder), "User should be banned");

        // 4. Finalize payment for an auction where the banned bidder won
        String result = walletService.finalizePaymentForWinner(
                UUID.randomUUID().toString(),
                bidder,
                seller,
                new BigDecimal("100.00"));

        // 5. Check that the payment was blocked due to the ban
        assertEquals("Bidder is banned. Payment blocked.", result);
    }
}
