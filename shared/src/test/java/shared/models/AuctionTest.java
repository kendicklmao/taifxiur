package shared.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.enums.AuctionStatus;
import shared.enums.ItemStatus;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests for Auction class
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

        // Use current time to ensure the auction is always running during the test
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(3600); // End 1 hour from now

        item = new Electronic("Laptop", "Old laptop", seller, new BigDecimal("1000"), "Dell", ItemStatus.USED);
        item.setMinIncrement(new BigDecimal("100000")); // Set a default minimum increment
        auction = new Auction("auc123", item, new BigDecimal("1000"), seller, startTime, endTime);
        auction.startScheduler();
    }

    // Test auction creation
    @Test
    public void testAuctionCreation() {
        assertNotNull(auction);
        assertEquals("auc123", auction.getId());
        assertEquals(item, auction.getItem());
        assertEquals(seller, auction.getSeller());
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }

    // Test manual bid placement
    @Test
    public void testManualBid() {
        boolean success = auction.placeBid(bidder1, new BigDecimal("1200000"));
        assertTrue(success);
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("1200000")));
        assertEquals(bidder1, auction.getHighestBidder());
    }

    // Test auto bid for same bidder
    @Test
    public void testAutoBidSelfBidding() {
        // Register auto-bid for bidder1
        auction.registerAutoBid(bidder1, new BigDecimal("5000000"));

        // Initial price after auto-bid registration
        BigDecimal price1 = auction.getCurrentPrice();
        assertTrue(price1.compareTo(new BigDecimal("1000")) >= 0);
        assertEquals(bidder1, auction.getHighestBidder());

        boolean success = auction.placeBid(bidder1, new BigDecimal("2000000")); // Place manual bid as same bidder
        assertTrue(success);
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("2000000")));

        // Price should NOT increase further because bidder1 is already winning
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("2000000")));
    }

    // Test auto bid competition between bidders
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

    // Test placing bid with invalid amount (below starting price)
    @Test
    public void testPlaceBidInvalidAmount() {
        boolean result = auction.placeBid(bidder1, new BigDecimal("999")); // Below startPrice — always invalid
        assertFalse(result);
    }

    // Test that first bid at exactly startPrice is accepted
    @Test
    public void testPlaceBidAtStartPrice() {
        boolean result = auction.placeBid(bidder1, new BigDecimal("1000")); // Exactly startPrice — valid first bid
        assertTrue(result);
        assertEquals(0, auction.getCurrentPrice().compareTo(new BigDecimal("1000")));
    }
}