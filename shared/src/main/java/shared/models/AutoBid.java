package shared.models;

import java.math.BigDecimal;
import java.time.Instant;

public class AutoBid implements Comparable<AutoBid> {
    private final Bidder bidder;//người đấu giá
    private final BigDecimal maxBid;//số tiền lớn nhất muốn đấu giá
    private final Instant timeStamp;//thời gian

    public AutoBid(Bidder bidder, BigDecimal maxBid) {
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.timeStamp = Instant.now();
    }

    public Bidder getBidder() {
        return bidder;
    }

    public BigDecimal getMaxBid() {
        return maxBid;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }

    @Override
    public int compareTo(AutoBid other) {
        int cmp = other.getMaxBid().compareTo(this.getMaxBid());
        if (cmp != 0) return cmp;
        return this.getTimeStamp().compareTo(other.getTimeStamp());
    }
}