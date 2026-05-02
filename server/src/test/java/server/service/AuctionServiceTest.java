package server.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import server.database.DatabaseConfig;
import shared.enums.AuctionStatus;
import shared.enums.ItemStatus;
import shared.models.Auction;
import shared.models.Bidder;
import shared.models.Electronic;
import shared.models.Seller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

public class AuctionServiceTest {

    private AuctionService auctionService;
    private UserService userService;
    private Seller seller;
    private Bidder bidder;

    @Before
    public void setUp() {
        auctionService = new AuctionService();
        userService = new UserService();
        userService.initializeDefaultUsers();
        seller = (Seller) userService.getUser("seller");
        bidder = (Bidder) userService.getUser("bidder");
    }

    @After
    public void tearDown() {
        DatabaseConfig.closeDataSource();
    }

    @Test
    public void testCreateAuction() {
        Electronic item = new Electronic("Test Item", "Description", seller, "Brand", ItemStatus.NEW, null, null, "url");
        Instant startTime = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant endTime = startTime.plus(1, ChronoUnit.HOURS);
        Auction auction = auctionService.createAuction(seller, item, new BigDecimal("100.00"), startTime, endTime);

        assertNotNull(auction);
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }
    
    @Test
    public void testPlaceBid() {
        Electronic item = new Electronic("Test Item", "Description", seller, "Brand", ItemStatus.NEW, null, null);
        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(1, ChronoUnit.HOURS);
        Auction auction = auctionService.createAuction(seller, item, new BigDecimal("100.00"), startTime, endTime);
        auction.setStatus(AuctionStatus.RUNNING);

        boolean result = auctionService.placeBid(auction.getId(), bidder, new BigDecimal("150.00"));
        assertTrue(result);
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("150.00")));
    }

    @Test
    public void testRegisterAutoBid() {
        Electronic item = new Electronic("Test Item", "Description", seller, "Brand", ItemStatus.NEW, null, null);
        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(1, ChronoUnit.HOURS);
        Auction auction = auctionService.createAuction(seller, item, new BigDecimal("100.00"), startTime, endTime);
        auction.setStatus(AuctionStatus.RUNNING);

        auctionService.registerAutoBid(auction.getId(), bidder, new BigDecimal("200.00"));
        assertNotNull(auction.getHighestBidder());
    }
}
