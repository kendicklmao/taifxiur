package shared.models;

import shared.enums.RequestStatus;

import java.math.BigDecimal;
import java.util.UUID;

public class WithdrawRequest {
    private final String id;//mã
    private final Seller seller;//người bán
    private final BigDecimal amount;//số tiền muốn rút
    private final BankInfo bankInfo; // số tài khoản / ngân hàng
    private RequestStatus status;//trạng thái

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
    public void approveWithdraw() {//đồng í yêu cầu rút tiền
        if (status != RequestStatus.PENDING)
            throw new IllegalStateException();
        status = RequestStatus.APPROVED;
    }
    public void rejectWithdraw() {//từ chối yêu cầu rút tiền
        if (status != RequestStatus.PENDING)
            throw new IllegalStateException();
        status = RequestStatus.REJECTED;
    }
}
