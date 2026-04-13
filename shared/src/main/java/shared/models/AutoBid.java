package shared.models;

import java.math.BigDecimal;
import java.time.Instant;

public class AutoBid {
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
}