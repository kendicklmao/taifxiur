package controller;

import model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {
    private final ConcurrentHashMap<String, Auction> auctions = new ConcurrentHashMap<>(); //lưu các cuộc giao dịch

    public Auction createAuction(Seller seller, Item item, BigDecimal startPrice, Instant startTime, Instant endTime) {//tạo giao dịch
        if (seller == null || seller.isBanned()) 
            throw new IllegalArgumentException();
        if (!item.getSeller().equals(seller))
            throw new IllegalArgumentException();
        if (!item.isValid()) {
            throw new IllegalArgumentException();
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
        if (id == null){
            throw new IllegalArgumentException();
        }
        return auctions.get(id);
    }
    public void registerAutoBid(String auctionId, Bidder bidder, BigDecimal maxBid){
        if (auctionId == null || bidder == null || maxBid == null){
            throw new IllegalArgumentException();
        }
        Auction auction = auctions.get(auctionId);
        if (auction == null){
            throw new IllegalArgumentException();
        }
        auction.registerAutoBid(bidder, maxBid);
    }
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        List<Auction> allAuctions = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getStatus() == status) {
                allAuctions.add(a);
            }
        }
        return allAuctions;
    }
    public void itemPaid(String auctionId, Bidder bidder) {
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