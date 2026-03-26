import java.util.*;
import java.time.*; 

public class BidTransaction {
    private Bidder bidder;
    private double amount;
    private Instant time;
    public BidTransaction(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.time = Instant.now();
    }
    public Bidder getBidder() {
        return bidder;
    }
    public double getAmount() {
        return amount;
    }
    public Instant getTime() {
        return time;
    }
}