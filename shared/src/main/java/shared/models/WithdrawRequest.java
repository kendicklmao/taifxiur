package shared.models;

import shared.enums.RequestStatus;
import shared.models.users.Seller;

import java.math.BigDecimal;
import java.util.UUID;

public class WithdrawRequest {
    private final String id; // Mã yêu cầu
    private final Seller seller; // Người bán
    private final BigDecimal amount; // Số tiền muốn rút
    private final BankInfo bankInfo; // Số tài khoản ngân hàng
    private RequestStatus status; // Trạng thái

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

    // Đồng ý yêu cầu rút tiền
    public void approveWithdraw() {
        if (status != RequestStatus.PENDING) {
            throw new IllegalStateException();
        }

        status = RequestStatus.APPROVED;
    }

    // Từ chối yêu cầu rút tiền
    public void rejectWithdraw() {
        if (status != RequestStatus.PENDING) {
            throw new IllegalStateException();
        }

        status = RequestStatus.REJECTED;
    }
}