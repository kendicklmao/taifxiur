package model;

import java.math.BigDecimal;
import java.util.UUID;

public class WithdrawRequest {
    private final String id;
    private final Seller seller;
    private final BigDecimal amount;
    private final BankInfo bankInfo; // số tài khoản / ngân hàng
    private RequestStatus status;
    
    public WithdrawRequest(Seller seller, BigDecimal amount, BankInfo bankInfo) {
        if (seller == null || amount == null || bankInfo == null) {
            throw new IllegalArgumentException();
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException();
        }
        this.id = UUID.randomUUID().toString();
        this.seller = seller;
        this.amount = amount;
        this.bankInfo = bankInfo;
        this.status = RequestStatus.PENDING;
    }
    public String getId() { 
        return id; 
    }
    public Seller getSeller() { 
        return seller; 
    }
    public BigDecimal getAmount() { 
        return amount; 
    }
    public BankInfo getBankInfo() { 
        return bankInfo; 
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