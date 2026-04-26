package server.service;

import org.junit.Test;
import org.junit.Before;
import shared.enums.Role;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.Assert.*;

public class BannedPaymentTest {
    private UserService userService;
    private WalletService walletService;

    @Before
    public void setUp() {
        userService = new UserService();
        walletService = new WalletService();
    }

    @Test
    public void testBannedUserCannotBeCharged() {
        String bidder = "bidder_" + UUID.randomUUID().toString().substring(0, 8);
        String seller = "seller_" + UUID.randomUUID().toString().substring(0, 8);
        String admin = "admin_" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Đăng ký người dùng
        userService.register(bidder, "Pass@123", bidder + "@test.com", "q", "a", "q", "a", Role.BIDDER);
        userService.register(seller, "Pass@123", seller + "@test.com", "q", "a", "q", "a", Role.SELLER);
        userService.register(admin, "Pass@123", admin + "@test.com", "q", "a", "q", "a", Role.ADMIN);

        // 2. Nạp tiền vào ví người đấu giá (Cần có tiền thì mới thực hiện thanh toán ở
        // bước 4 được)
        walletService.createDepositRequest(bidder, new BigDecimal("1000"));

        // Lấy ID của yêu cầu vừa tạo để phê duyệt
        String requestId = walletService.getPendingDepositRequests().stream()
                .filter(r -> r.get("username").equals(bidder))
                .findFirst()
                .get()
                .get("id");
        walletService.approveDeposit(requestId, admin);

        // 3. Ban người đấu giá
        userService.banUser(bidder, admin);
        assertTrue("User should be banned", userService.isUserBanned(bidder));

        // 4. Thanh toán cho phiên đấu giá nơi người đấu giá bị cấm đã thắng
        String result = walletService.finalizePaymentForWinner(
                UUID.randomUUID().toString(),
                bidder,
                seller,
                new BigDecimal("100.00"));

        // 5. Kiểm tra thanh toán đã bị chặn do lệnh cấm
        assertEquals("Bidder is banned. Payment blocked.", result);
    }
}
