package model;

import java.math.BigDecimal;
import java.util.UUID;

public class DepositRequest {
    private final String id;
    private final Bidder bidder;
    private final BigDecimal amount;
    private RequestStatus status;

    public DepositRequest(Bidder bidder, BigDecimal amount) {
        if (bidder == null || amount == null) {
            throw new IllegalArgumentException();
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException();
        }
        this.id = UUID.randomUUID().toString();
        this.bidder = bidder;
        this.amount = amount;
        this.status = RequestStatus.PENDING;
    }
    public String getId(){
        return id;
    }
    public Bidder getBidder() { 
        return bidder; 
    }
    public BigDecimal getAmount() { 
        return amount; 
    }
    public RequestStatus getStatus() { 
        return status; 
    }

    public void approve() {
        if (status != RequestStatus.PENDING) 
            throw new IllegalStateException();
        status = RequestStatus.APPROVED;
    }

    public void reject() {
        if (status != RequestStatus.PENDING) 
            throw new IllegalStateException();
        status = RequestStatus.REJECTED;
    }
}