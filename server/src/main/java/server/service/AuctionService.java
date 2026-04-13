package server.service;

import shared.enums.AuctionStatus;
import shared.models.Auction;
import shared.models.Bidder;
import shared.models.Item;
import shared.models.Seller;
import shared.models.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {
    private final ConcurrentHashMap<String, Auction> auctions = new ConcurrentHashMap<>(); //lưu các cuộc giao dịch

    public Auction createAuction(Seller seller, Item item, BigDecimal startPrice, Instant startTime, Instant endTime) {//tạo giao dịch
        if (seller == null) 
            throw new IllegalArgumentException("Seller is null");
        if (seller.isBanned())
            throw new IllegalArgumentException("Seller is banned");
        if (item == null)
            throw new IllegalArgumentException("Item is null");
        if (!item.getSeller().equals(seller))
            throw new IllegalArgumentException("Item seller mismatch");
        if (!item.isValid()) {
            throw new IllegalArgumentException("Item is invalid");
        }
        String id = UUID.randomUUID().toString();
        Auction auction = new Auction(id, item, startPrice, seller, startTime, endTime);
        auctions.put(id, auction);
        return auction;
    }
    public boolean placeBid(String auctionId, Bidder bidder, BigDecimal amount) {//đặt giá
        if (auctionId == null || bidder == null || amount == null) throw new IllegalArgumentException();
        Auction auction = auctions.get(auctionId);
        if (auction == null){
            throw new IllegalArgumentException();
        }
        return auction.placeBid(bidder, amount);
    }
    public Auction getAuction(String id) {
        if (id == null) return null;
        return auctions.get(id);
    }
    public void registerAutoBid(String auctionId, Bidder bidder, BigDecimal maxBid){//đăng kí autobid
        if (auctionId == null || bidder == null || maxBid == null){
            throw new IllegalArgumentException();
        }
        Auction auction = auctions.get(auctionId);
        if (auction == null){
            throw new IllegalArgumentException();
        }
        auction.registerAutoBid(bidder, maxBid);
    }
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {//xem các phiên giao dịch theo trạng thái
        List<Auction> allAuctions = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getStatus() == status) {
                allAuctions.add(a);
            }
        }
        return allAuctions;
    }
    public void itemPaid(String auctionId, Bidder bidder) {// bidder thắng trả tiền seller
        if (auctionId == null || bidder == null){
            throw new IllegalArgumentException();
        }
        Auction auction = auctions.get(auctionId);
        if (auction == null){
            throw new IllegalArgumentException();
        }
        auction.itemPaid(bidder);
    }
}