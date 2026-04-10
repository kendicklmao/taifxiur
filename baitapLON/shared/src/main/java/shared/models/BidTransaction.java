package shared.models;

import java.math.BigDecimal;
import java.time.Instant;

public class BidTransaction { //1 lần đặt giá
    private final Bidder bidder; //người đấu giá
    private final BigDecimal amount;//giá trị người đấu giá đặt
    private final Instant time; //đặt giá tại thời điểm?

    public BidTransaction(Bidder bidder, BigDecimal amount, Instant time) {
        this.bidder = bidder;
        this.amount = amount;
        this.time = time;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getTime() {
        return time;
    }
}