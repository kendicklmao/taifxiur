package server.service;

import org.junit.jupiter.api.*;
import server.database.DatabaseConfig;
import server.database.DatabaseInitializer;
import shared.enums.AuctionStatus;
import shared.enums.ItemStatus;
import shared.models.Auction;
import shared.models.Bidder;
import shared.models.Electronic;
import shared.models.Seller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionServiceTest {

    private static AuctionService auctionService;
    private static UserService userService;
    private static WalletService walletService;
    private Seller seller;
    private Bidder bidder;

    @BeforeAll
    public static void setUpClass() throws Exception {
        DatabaseInitializer.initializeDatabase();
        // Dọn dẹp DB một lần trước khi khởi tạo service để tránh lỗi "min_increment is null"
        cleanupDatabaseOnce(); 
        userService = new UserService();
        walletService = new WalletService();
        auctionService = new AuctionService(userService, walletService);
        userService.setWalletService(walletService);
    }

    @BeforeEach
    public void setUp() {
        cleanupDatabase();
        auctionService.clearCache(); // Xóa cache trước mỗi test
        userService.initializeDefaultUsers();
        seller = (Seller) userService.getUser("seller");
        bidder = (Bidder) userService.getUser("bidder");

        // Đảm bảo bidder có tiền để test
        walletService.createDepositRequest(bidder.getUsername(), new BigDecimal("1000.00"), "Test Bank", "12345");
        String requestId = walletService.getPendingDepositRequests().get(0).get("id");
        walletService.approveDeposit(requestId, "admin");
    }

    @AfterEach
    public void tearDown() {
        cleanupDatabase();
        auctionService.clearCache(); // Xóa cache sau mỗi test
    }

    @AfterAll
    public static void tearDownClass() {
        DatabaseConfig.closeDataSource();
    }

    private void cleanupDatabase() {
        // FIXME: Commented out to prevent wiping the actual database during tests
        // try (java.sql.Connection conn = DatabaseConfig.getDataSource().getConnection();
        //      java.sql.Statement stmt = conn.createStatement()) {
        //     stmt.executeUpdate("DELETE FROM bids");
        //     stmt.executeUpdate("DELETE FROM auto_bids");
        //     stmt.executeUpdate("DELETE FROM items");
        //     stmt.executeUpdate("DELETE FROM wallets");
        //     stmt.executeUpdate("DELETE FROM users");
        // } catch (java.sql.SQLException e) {
        //     e.printStackTrace();
        // }
    }
    
    private static void cleanupDatabaseOnce() {
        // FIXME: Commented out to prevent wiping the actual database during tests
        // try (java.sql.Connection conn = DatabaseConfig.getDataSource().getConnection();
        //      java.sql.Statement stmt = conn.createStatement()) {
        //     stmt.executeUpdate("DELETE FROM bids");
        //     stmt.executeUpdate("DELETE FROM auto_bids");
        //     stmt.executeUpdate("DELETE FROM items");
        //     stmt.executeUpdate("DELETE FROM wallets");
        //     stmt.executeUpdate("DELETE FROM users");
        // } catch (java.sql.SQLException e) {
        //     e.printStackTrace();
        // }
    }

    @Test
    public void testCreateAuction() {
        Electronic item = new Electronic("Test Item", "Description", seller, new BigDecimal("10.00"), "Brand", ItemStatus.NEW);
        item.setMinIncrement(new BigDecimal("10.00")); // Thêm dòng này
        Instant startTime = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant endTime = startTime.plus(1, ChronoUnit.HOURS);
        Auction auction = auctionService.createAuction(seller, item, new BigDecimal("100.00"), startTime, endTime);

        assertNotNull(auction);
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }
    
    @Test
    public void testPlaceBid() {
        Electronic item = new Electronic("Test Item", "Description", seller, new BigDecimal("10.00"), "Brand", ItemStatus.NEW);
        item.setMinIncrement(new BigDecimal("10.00")); // Thêm dòng này
        Instant startTime = Instant.now().minus(1, ChronoUnit.MINUTES); // Auction starts immediately
        Instant endTime = startTime.plus(1, ChronoUnit.HOURS);
        Auction auction = auctionService.createAuction(seller, item, new BigDecimal("100.00"), startTime, endTime);

        boolean result = auctionService.placeBid(auction.getId(), bidder, new BigDecimal("150.00"));
        assertTrue(result);
        
        Auction updatedAuction = auctionService.getAuction(auction.getId());
        assertEquals(0, updatedAuction.getCurrentPrice().compareTo(new BigDecimal("150.00")));
    }

    @Test
    public void testRegisterAutoBid() {
        Electronic item = new Electronic("Test Item", "Description", seller, new BigDecimal("10.00"), "Brand", ItemStatus.NEW);
        item.setMinIncrement(new BigDecimal("10.00")); // Thêm dòng này
        Instant startTime = Instant.now().minus(1, ChronoUnit.MINUTES); // Auction starts immediately
        Instant endTime = startTime.plus(1, ChronoUnit.HOURS);
        Auction auction = auctionService.createAuction(seller, item, new BigDecimal("100.00"), startTime, endTime);

        auctionService.registerAutoBid(auction.getId(), bidder, new BigDecimal("200.00"));
        
        Auction updatedAuction = auctionService.getAuction(auction.getId());
        assertNotNull(updatedAuction.getHighestBidder());
        assertEquals(bidder.getUsername(), updatedAuction.getHighestBidder().getUsername());

        // Sửa lỗi: Kỳ vọng giá hiện tại là giá khởi điểm + một bước giá
        BigDecimal expectedPrice = new BigDecimal("100.00").add(item.getMinIncrement());
        assertEquals(0, updatedAuction.getCurrentPrice().compareTo(expectedPrice));
    }
}
