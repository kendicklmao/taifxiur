package server.service;

import org.junit.jupiter.api.*;
import server.database.DatabaseConfig;
import server.database.DatabaseInitializer;
import shared.enums.AuctionStatus;
import shared.enums.ItemStatus;
import shared.enums.Role;
import shared.models.Auction;
import shared.models.items.Electronic;
import shared.models.users.Bidder;
import shared.models.users.Seller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionServiceTest {

    private static AuctionService auctionService;
    private static UserService userService;
    private static WalletService walletService;
    private Seller seller;
    private Bidder bidder;

    private String testSeller;
    private String testBidder;
    private String testAdmin;
    private List<String> createdAuctionIds;

    @BeforeAll
    public static void setUpClass() throws Exception {
        DatabaseInitializer.initializeDatabase();
        userService = new UserService();
        walletService = new WalletService();
        auctionService = new AuctionService(userService, walletService);
    }

    @BeforeEach
    public void setUp() {
        auctionService.clearCache();
        createdAuctionIds = new ArrayList<>();
        
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        testSeller = "seller_" + suffix;
        testBidder = "bidder_" + suffix;
        testAdmin = "admin_" + suffix;

        userService.register(testSeller, "Password@123", "seller" + suffix + "@test.com", "q", "a", "q", "a", Role.SELLER);
        userService.register(testBidder, "Password@123", "bidder" + suffix + "@test.com", "q", "a", "q", "a", Role.BIDDER);
        userService.register(testAdmin, "Password@123", "admin" + suffix + "@test.com", "q", "a", "q", "a", Role.ADMIN);

        seller = (Seller) userService.getUser(testSeller);
        bidder = (Bidder) userService.getUser(testBidder);

        walletService.createDepositRequest(testBidder, new BigDecimal("1000.00"), "Test Bank", "12345");
        String requestId = walletService.getPendingDepositRequests().stream()
                .filter(r -> r.get("username").equals(testBidder))
                .findFirst()
                .map(r -> r.get("id"))
                .orElse(null);
                
        if (requestId != null) {
            walletService.approveDeposit(requestId, testAdmin);
        }
    }

    @AfterEach
    public void tearDown() {
        cleanupDatabase();
        auctionService.clearCache();
    }

    @AfterAll
    public static void tearDownClass() {
        DatabaseConfig.closeDataSource();
    }

    private void cleanupDatabase() {
        try (java.sql.Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (String auctionId: createdAuctionIds) {
                    try (java.sql.PreparedStatement pstmt = conn.prepareStatement("DELETE FROM bids WHERE auction_id = ?")) {
                        pstmt.setString(1, auctionId);
                        pstmt.executeUpdate();
                    }
                    try (java.sql.PreparedStatement pstmt = conn.prepareStatement("DELETE FROM auto_bids WHERE auction_id = ?")) {
                        pstmt.setString(1, auctionId);
                        pstmt.executeUpdate();
                    }
                    try (java.sql.PreparedStatement pstmt = conn.prepareStatement("DELETE FROM items WHERE auction_id = ?")) {
                        pstmt.setString(1, auctionId);
                        pstmt.executeUpdate();
                    }
                }

                try (java.sql.PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE username IN (?, ?, ?)")) {
                    pstmt.setString(1, testSeller);
                    pstmt.setString(2, testBidder);
                    pstmt.setString(3, testAdmin);
                    pstmt.executeUpdate();
                }

                conn.commit();
            } catch (java.sql.SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateAuction() {
        Electronic item = new Electronic("Test Item", "Description", seller, new BigDecimal("10.00"), "Brand",
                ItemStatus.NEW);
        item.setMinIncrement(new BigDecimal("10.00"));
        Instant startTime = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant endTime = startTime.plus(1, ChronoUnit.HOURS);
        Auction auction = auctionService.createAuction(seller, item, new BigDecimal("100.00"), startTime, endTime);

        if (auction != null) {
            createdAuctionIds.add(auction.getId());
        }

        assertNotNull(auction);
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    public void testPlaceBid() {
        Electronic item = new Electronic("Test Item", "Description", seller, new BigDecimal("10.00"), "Brand",
                ItemStatus.NEW);
        item.setMinIncrement(new BigDecimal("10.00"));
        Instant startTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant endTime = startTime.plus(1, ChronoUnit.HOURS);
        Auction auction = auctionService.createAuction(seller, item, new BigDecimal("100.00"), startTime, endTime);

        if (auction != null) {
            createdAuctionIds.add(auction.getId());
        }

        boolean result = auctionService.placeBid(auction.getId(), bidder, new BigDecimal("150.00"));
        assertTrue(result);

        Auction updatedAuction = auctionService.getAuction(auction.getId());
        assertEquals(0, updatedAuction.getCurrentPrice().compareTo(new BigDecimal("150.00")));
    }

    @Test
    public void testRegisterAutoBid() {
        Electronic item = new Electronic("Test Item", "Description", seller, new BigDecimal("10.00"), "Brand",
                ItemStatus.NEW);
        item.setMinIncrement(new BigDecimal("10.00"));
        Instant startTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant endTime = startTime.plus(1, ChronoUnit.HOURS);
        Auction auction = auctionService.createAuction(seller, item, new BigDecimal("100.00"), startTime, endTime);

        if (auction != null) {
            createdAuctionIds.add(auction.getId());
        }

        auctionService.registerAutoBid(auction.getId(), bidder, new BigDecimal("200.00"));

        Auction updatedAuction = auctionService.getAuction(auction.getId());
        assertNotNull(updatedAuction.getHighestBidder());
        assertEquals(bidder.getUsername(), updatedAuction.getHighestBidder().getUsername());

        BigDecimal expectedPrice = new BigDecimal("100.00").add(item.getMinIncrement());
        assertEquals(-1, updatedAuction.getCurrentPrice().compareTo(expectedPrice));
    }

    @Test
    public void testAutoSnipingExtension() {
        Electronic item = new Electronic("Test Sniping Item", "Description", seller, new BigDecimal("10.00"), "Brand",
                ItemStatus.NEW);
        item.setMinIncrement(new BigDecimal("10.00"));
        Instant startTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant originalEndTime = Instant.now().plus(5, ChronoUnit.SECONDS);
        Auction auction = auctionService.createAuction(seller, item, new BigDecimal("100.00"), startTime, originalEndTime);

        if (auction != null) {
            createdAuctionIds.add(auction.getId());
        }

        boolean result = auctionService.placeBid(auction.getId(), bidder, new BigDecimal("150.00"));
        assertTrue(result);

        Auction updatedAuction = auctionService.getAuction(auction.getId());
        Instant extendedEndTime = updatedAuction.getEndTime();
        assertTrue(extendedEndTime.isAfter(originalEndTime));

        try (java.sql.Connection conn = DatabaseConfig.getDataSource().getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT end_time FROM items WHERE auction_id = ?")) {
            pstmt.setString(1, auction.getId());
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                assertTrue(rs.next());
                Instant dbEndTime = rs.getTimestamp("end_time").toInstant();
                assertEquals(extendedEndTime.getEpochSecond(), dbEndTime.getEpochSecond());
            }
        } catch (java.sql.SQLException e) {
            fail("Database error: " + e.getMessage());
        }
    }
}