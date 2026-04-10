package model;

import controller.AuctionService;
import controller.WalletService;

import java.math.BigDecimal;

public class Bidder extends User { //người đấu giá
    private Wallet wallet = new Wallet();

    public Bidder(String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(username, password, email, Role.BIDDER, q1, a1, q2, a2);
    }
    public boolean placeBid(AuctionService service, String auctionId, BigDecimal amount) { //khả năng đấu giá
        if (service == null || auctionId == null || amount == null){
            throw new IllegalArgumentException();
        }
        return service.placeBid(auctionId, this, amount);
    }
    public void registerAutoBid(AuctionService service, String auctionId, BigDecimal maxBid){
        if (service == null || maxBid == null || auctionId == null){
            throw new IllegalArgumentException();
        }
        service.registerAutoBid(auctionId, this, maxBid);
    }
    public void itemPaid(AuctionService service, String auctionId) {
        if (service == null || auctionId == null) {
            throw new IllegalArgumentException();
        }
        service.itemPaid(auctionId, this);
    }
    public void createRequest(WalletService service, BigDecimal amount) {
        if (service == null || amount == null) {
            throw new IllegalArgumentException();
        }
        service.createDepositRequest(this, amount);
    }
    public Wallet getWallet(){
        return wallet;
    }
}