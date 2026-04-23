package shared.models;

import org.junit.Before;
import org.junit.Test;
import shared.enums.AuctionStatus;
import shared.enums.ItemStatus;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.Assert.*;

/**
 * Unit tests for Auction class
 */
public class AuctionTest {
    private Seller seller;
    private Bidder bidder1;
    private Bidder bidder2;
    private Electronic item;
    private Auction auction;

    @Before
    public void setUp() {
        seller = new Seller("seller123", "Pass@123", "s@mail.com", "q", "a", "q", "a");
        bidder1 = new Bidder("bidder1", "Pass@123", "b1@mail.com", "q", "a", "q", "a");
        bidder2 = new Bidder("bidder2", "Pass@123", "b2@mail.com", "q", "a", "q", "a");
        item = new Electronic("Laptop", "Old laptop", seller, "Dell", ItemStatus.USED);

        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now().plusSeconds(60);
        auction = new Auction("auc123", item, new BigDecimal("1000"), seller, start, end);

        // Wait for it to be RUNNING
        int maxWait = 0;
        while (auction.getStatus() != AuctionStatus.RUNNING && maxWait < 20) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            maxWait++;
        }
    }

    /**
     * Test auction creation
     */
    @Test
    public void testAuctionCreation() {
        assertNotNull(auction);
        assertEquals("auc123", auction.getId());
        assertEquals(item, auction.getItem());
        assertEquals(seller, auction.getSeller());
    }

    /**
     * Test manual bid placement
     */
    @Test
    public void testManualBid() {
        boolean success = auction.placeBid(bidder1, new BigDecimal("1200000"));
        assertTrue(success);
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("1200000")));
        assertEquals(bidder1, auction.getHighestBidder());
    }

    /**
     * Test auto bid for same bidder
     */
    @Test
    public void testAutoBidSelfBidding() {
        // Register auto-bid for bidder1
        auction.registerAutoBid(bidder1, new BigDecimal("5000000"));
        
        // Initial price after auto-bid registration
        BigDecimal price1 = auction.getCurrentPrice();
        assertTrue(price1.compareTo(new BigDecimal("1000")) >= 0);
        assertEquals(bidder1, auction.getHighestBidder());
        
        // Place manual bid as same bidder
        boolean success = auction.placeBid(bidder1, new BigDecimal("2000000"));
        assertTrue(success);
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("2000000")));

        // Price should NOT increase further because bidder1 is already winning
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("2000000")));
    }

    /**
     * Test auto bid competition between bidders
     */
    @Test
    public void testAutoBidCompetition() {
        // Bidder 1 max 2,000,000
        auction.registerAutoBid(bidder1, new BigDecimal("2000000"));
        assertEquals(bidder1, auction.getHighestBidder());
        
        // Bidder 2 max 3,000,000
        auction.registerAutoBid(bidder2, new BigDecimal("3000000"));
        
        assertEquals(bidder2, auction.getHighestBidder());
        // Price should be bidder1's max + increment = 2,100,000
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("2100000")));
    }

    /**
     * Test placing bid with invalid amount
     */
    @Test
    public void testPlaceBidInvalidAmount() {
        // Try to bid less than minimum increment
        boolean result = auction.placeBid(bidder1, new BigDecimal("1000"));
        assertFalse(result);
    }
}
