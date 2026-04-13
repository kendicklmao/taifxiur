package shared.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.enums.AuctionStatus;
import shared.models.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {
    private Seller seller;
    private Bidder bidder1;
    private Bidder bidder2;
    private Electronic item;
    private Auction auction;

    @BeforeEach
    void setUp() {
        seller = new Seller("seller123", "Pass@123", "s@mail.com", "q", "a", "q", "a");
        bidder1 = new Bidder("bidder1", "Pass@123", "b1@mail.com", "q", "a", "q", "a");
        bidder2 = new Bidder("bidder2", "Pass@123", "b2@mail.com", "q", "a", "q", "a");
        item = new Electronic("Laptop", "Old laptop", seller, "Dell", shared.enums.ItemStatus.USED);
        
        Instant start = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant end = Instant.now().plus(1, ChronoUnit.MINUTES);
        auction = new Auction("auc123", item, new BigDecimal("1000"), seller, start, end);
        // Wait for it to be RUNNING
        while (auction.getStatus() != AuctionStatus.RUNNING) {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
    }

    @Test
    void testManualBid() {
        boolean success = auction.placeBid(bidder1, new BigDecimal("1200000"));
        assertTrue(success);
        assertEquals(new BigDecimal("1200000"), auction.getCurrentPrice());
        assertEquals(bidder1, auction.getHighestBidder());
    }

    @Test
    void testAutoBidSelfBidding() {
        // Register auto-bid for bidder1
        auction.registerAutoBid(bidder1, new BigDecimal("5000000"));
        
        // Initial price after auto-bid registration (it outbids current price if no one else is bidding)
        // Item min increment is 100k
        BigDecimal price1 = auction.getCurrentPrice();
        assertTrue(price1.compareTo(new BigDecimal("1000")) >= 0);
        assertEquals(bidder1, auction.getHighestBidder());
        
        // Place manual bid as same bidder
        boolean success = auction.placeBid(bidder1, new BigDecimal("2000000"));
        assertTrue(success);
        assertEquals(new BigDecimal("2000000"), auction.getCurrentPrice());
        
        // Call AutoBidService again (should have been called by placeBid)
        // Price should NOT increase further because bidder1 is already winning
        assertEquals(new BigDecimal("2000000"), auction.getCurrentPrice());
    }

    @Test
    void testAutoBidCompetition() {
        // Bidder 1 max 2,000,000
        auction.registerAutoBid(bidder1, new BigDecimal("2000000"));
        assertEquals(bidder1, auction.getHighestBidder());
        
        // Bidder 2 max 3,000,000
        auction.registerAutoBid(bidder2, new BigDecimal("3000000"));
        
        assertEquals(bidder2, auction.getHighestBidder());
        // Price should be bidder1's max + increment = 2,100,000
        assertEquals(new BigDecimal("2100000"), auction.getCurrentPrice());
    }
}
