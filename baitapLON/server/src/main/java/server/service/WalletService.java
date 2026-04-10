package server.service;

import shared.enums.RequestStatus;
import shared.models.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WalletService {
    private final ConcurrentHashMap<String, DepositRequest> depositRequests = new ConcurrentHashMap<>();//danh sách yêu cầu nạp tiền
    private final ConcurrentHashMap<String, WithdrawRequest> withdrawRequests = new ConcurrentHashMap<>();//danh sách yêu cầu rút tiền

    public void createDepositRequest(Bidder bidder, BigDecimal amount) {//tạo yêu cầu nạp tiền
        if (bidder == null){
            throw new IllegalArgumentException();
        }
        DepositRequest req = new DepositRequest(bidder, amount);
        depositRequests.put(req.getId(), req);
    }
    public List<DepositRequest> getPendingDepositRequests() {//xem danh sách yêu cầu nạp tiền
        List<DepositRequest> listDepositRequest = new ArrayList<>();
        for (DepositRequest r : depositRequests.values()) {
            if (r.getStatus() == RequestStatus.PENDING) {
                listDepositRequest.add(r);
            }
        }
        return listDepositRequest;
    }
    public List<WithdrawRequest> getPendingWithdrawRequests(){//xem danh sachs yêu cầu rút tiền
        List<WithdrawRequest> listWithdrawRequest = new ArrayList<>();
        for (WithdrawRequest r : withdrawRequests.values()){
            if (r.getStatus() == RequestStatus.PENDING){
                listWithdrawRequest.add(r);
            }
        }
        return listWithdrawRequest;
    }
    public void approveDeposit(String requestId, Admin admin) {//đồng í yêu cầu nạp tiền
        if (admin == null || requestId == null) {
            throw new IllegalArgumentException();
        }
        DepositRequest req = depositRequests.get(requestId);
        if (req == null || req.getStatus() != RequestStatus.PENDING) 
            throw new IllegalArgumentException();
        req.approveDeposit();
        req.getBidder().getWallet().deposit(req.getAmount());
    }
    public void rejectDeposit(String requestId, Admin admin) {//từ chối yêu cầu nạp tiền
        if (admin == null || requestId == null) {
            throw new IllegalArgumentException();
        }
        DepositRequest req = depositRequests.get(requestId);
        if (req == null || req.getStatus() != RequestStatus.PENDING) 
            throw new IllegalArgumentException();
        req.rejectDeposit();
    }
    public void approveWithdraw(String requestId, Admin admin){//đồng í yêu cầu rút tiền
        if (admin == null || requestId == null){
            throw new IllegalArgumentException();
        }
        WithdrawRequest req = withdrawRequests.get(requestId);
        if (req == null || req.getStatus() != RequestStatus.PENDING){
            throw new IllegalArgumentException();
        }
        req.approveWithdraw();
    }
    public void rejectWithdraw(String requestId, Admin admin){//từ chối yêu cầu rút tiền
        if (admin == null || requestId == null){
            throw new IllegalArgumentException();
        }
        WithdrawRequest req = withdrawRequests.get(requestId);
        if (req == null || req.getStatus() != RequestStatus.PENDING){
            throw new IllegalArgumentException();
        }
        req.rejectWithdraw();
    }
    public void createWithdrawRequest(Seller seller, BigDecimal amount, BankInfo bankInfo){//tạo yêu cầu rút tiền
        if (seller == null){
            throw new IllegalArgumentException();
        }
        WithdrawRequest req = new WithdrawRequest(seller, amount, bankInfo);
        withdrawRequests.put(req.getId(), req);
    }
}