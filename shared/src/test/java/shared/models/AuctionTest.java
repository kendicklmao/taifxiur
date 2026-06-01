package shared.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.enums.AuctionStatus;
import shared.enums.ItemStatus;
import shared.models.items.Electronic;
import shared.models.users.Bidder;
import shared.models.users.Seller;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {
    private Seller seller;
    private Bidder bidder1;
    private Bidder bidder2;
    private Electronic item;
    private Auction auction;

    @BeforeEach
    public void setUp() {
        seller = new Seller("seller123", "Pass@123", "s@mail.com", "q", "a", "q", "a");
        bidder1 = new Bidder("bidder1", "Pass@123", "b1@mail.com", "q", "a", "q", "a");
        bidder2 = new Bidder("bidder2", "Pass@123", "b2@mail.com", "q", "a", "q", "a");

        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(3600);

        item = new Electronic("Laptop", "Old laptop", seller, new BigDecimal("1000"), "Dell", ItemStatus.USED);
        item.setMinIncrement(new BigDecimal("100000"));
        auction = new Auction("auc123", item, new BigDecimal("1000"), seller, startTime, endTime);
        auction.startScheduler();
    }

    @Test
    public void testAuctionCreation() {
        assertNotNull(auction);
        assertEquals("auc123", auction.getId());
        assertEquals(item, auction.getItem());
        assertEquals(seller, auction.getSeller());
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    public void testManualBid() {
        boolean success = auction.placeBid(bidder1, new BigDecimal("1200000"));
        assertTrue(success);
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("1200000")));
        assertEquals(bidder1, auction.getHighestBidder());
    }

    @Test
    public void testAutoBidSelfBidding() {
        auction.registerAutoBid(bidder1, new BigDecimal("5000000"));

        BigDecimal price1 = auction.getCurrentPrice();
        assertTrue(price1.compareTo(new BigDecimal("1000")) >= 0);
        assertEquals(bidder1, auction.getHighestBidder());

        boolean success = auction.placeBid(bidder1, new BigDecimal("2000000"));
        assertTrue(success);
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("2000000")));

        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("2000000")));
    }

    @Test
    public void testAutoBidCompetition() {
        auction.registerAutoBid(bidder1, new BigDecimal("2000000"));
        assertEquals(bidder1, auction.getHighestBidder());

        auction.registerAutoBid(bidder2, new BigDecimal("3000000"));

        assertEquals(bidder2, auction.getHighestBidder());
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("2100000")));
    }

    @Test
    public void testPlaceBidInvalidAmount() {
        boolean result = auction.placeBid(bidder1, new BigDecimal("999"));
        assertFalse(result);
    }

    @Test
    public void testPlaceBidAtStartPrice() {
        boolean result = auction.placeBid(bidder1, new BigDecimal("1000"));
        assertTrue(result);
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("1000")));
    }
}