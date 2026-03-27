import java.time.Instant;

public class BidTransaction {
    private Bidder bidder;
    private double amount;
    private Instant time;

    public BidTransaction(Bidder bidder, double amount, Instant time) {
        this.bidder = bidder;
        this.amount = amount;
        this.time = time;
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