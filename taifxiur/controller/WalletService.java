package controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import model.*;

public class WalletService {
    private final ConcurrentHashMap<String, DepositRequest> depositRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WithdrawRequest> withdrawRequests = new ConcurrentHashMap<>();

    public void createDepositRequest(Bidder bidder, BigDecimal amount) {
        if (bidder == null){
            throw new IllegalArgumentException();
        }
        DepositRequest req = new DepositRequest(bidder, amount);
        depositRequests.put(req.getId(), req);
    }
    public List<DepositRequest> getPendingDepositRequests() {
        List<DepositRequest> listDepositRequest = new ArrayList<>();
        for (DepositRequest r : depositRequests.values()) {
            if (r.getStatus() == RequestStatus.PENDING) {
                listDepositRequest.add(r);
            }
        }
        return listDepositRequest;
    }
    public void approveDeposit(String requestId, Admin admin) {
        if (admin == null || admin.isBanned()) {
            throw new IllegalArgumentException();
        }
        DepositRequest req = depositRequests.get(requestId);
        if (req == null) 
            throw new IllegalArgumentException();
        req.approve();
        req.getBidder().getWallet().deposit(req.getAmount());
    }
    public void rejectDeposit(String requestId, Admin admin) {
        if (admin == null || admin.isBanned()) {
            throw new IllegalArgumentException();
        }
        DepositRequest req = depositRequests.get(requestId);
        if (req == null) throw new IllegalArgumentException();
        req.reject();
    }
    public void approveWithdraw(String requestId, Admin admin){
        if (admin == null || admin.isBanned()){
            throw new IllegalArgumentException();
        }
        WithdrawRequest req = withdrawRequests.get(requestId);
        if (req == null){
            throw new IllegalArgumentException();
        }
        req.approve();
    }
    public void rejectWithdraw(String requestId, Admin admin){
        if (admin == null || admin.isBanned()){
            throw new IllegalArgumentException();
        }
        WithdrawRequest req = withdrawRequests.get(requestId);
        if (req == null){
            throw new IllegalArgumentException();
        }
        req.reject();
    }
    public void createWithdrawRequest(Seller seller, BigDecimal amount, BankInfo bankInfo){
        if (seller == null){
            throw new IllegalArgumentException();
        }
        WithdrawRequest req = new WithdrawRequest(seller, amount, bankInfo);
        withdrawRequests.put(req.getId(), req);
    }
}