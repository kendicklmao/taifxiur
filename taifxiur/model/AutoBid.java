package model;

import java.math.BigDecimal;
import java.time.*;

public class AutoBid {
    private Bidder bidder;
    private BigDecimal maxBid;
    private Instant timeStamp;

    public AutoBid(Bidder bidder, BigDecimal maxBid) {
        this.bidder = bidder;
        this.maxBid = maxBid;
        this.timeStamp = Instant.now();
    }
    public Bidder getBidder(){
        return bidder;
    }
    public BigDecimal getMaxBid(){
        return maxBid;
    }
    public Instant getTimeStamp(){
        return timeStamp;
    }
}
