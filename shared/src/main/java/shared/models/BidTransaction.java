package shared.models;

import java.math.BigDecimal;
import java.time.Instant;

import shared.models.users.Bidder;

// Lần đặt giá
public class BidTransaction {
    private final Bidder bidder; // Người đấu giá
    private final BigDecimal amount; // Giá trị người đấu giá đặt
    private final Instant time; // Đặt giá tại thời điểm

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