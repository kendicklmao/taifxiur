package shared.models;

import shared.enums.RequestStatus;
import shared.models.users.Bidder;

import java.math.BigDecimal;
import java.util.UUID;

public class DepositRequest {
    private final String id; // Mã số yêu cầu
    private Bidder bidder; // Người đấu giá
    private BigDecimal amount; // Số tiền yêu cầu nạp
    private RequestStatus status; // Trạng thái yêu cầu

    public DepositRequest(Bidder bidder, BigDecimal amount) {
        if (bidder == null || amount == null) {
            throw new IllegalArgumentException("Bidder and amount cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
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

    public void approveDeposit() {
        if (status != RequestStatus.PENDING) {
            throw new IllegalStateException();
        }
        status = RequestStatus.APPROVED;
    }

    public void rejectDeposit() {
        if (status != RequestStatus.PENDING) {
            throw new IllegalStateException();
        }
        status = RequestStatus.REJECTED;
    }
}