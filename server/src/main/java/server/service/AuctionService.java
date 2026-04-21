package server.service;

import server.database.DatabaseConfig;
import shared.enums.AuctionStatus;
import shared.models.Auction;
import shared.models.Bidder;
import shared.models.Item;
import shared.models.Seller;
import shared.models.User;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {
    private final ConcurrentHashMap<String, Auction> auctions = new ConcurrentHashMap<>(); //lưu các cuộc giao dịch

    /**
     * Create auction and store in database
     */
    public Auction createAuction(Seller seller, Item item, BigDecimal startPrice, Instant startTime, Instant endTime) {
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

        // Store item in database first
        int itemId = saveItemToDatabase(item, seller);

        // Store auction in database
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO auctions (id, item_id, seller_id, start_price, current_price, status, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {

            pstmt.setString(1, id);
            pstmt.setInt(2, itemId); // Use actual item ID from database
            pstmt.setInt(3, getUserIdFromDatabase(seller.getUsername()));
            pstmt.setBigDecimal(4, startPrice);
            pstmt.setBigDecimal(5, startPrice);
            pstmt.setString(6, AuctionStatus.OPEN.name());
            pstmt.setTimestamp(7, Timestamp.from(startTime));
            pstmt.setTimestamp(8, Timestamp.from(endTime));

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating auction: " + e.getMessage());
            e.printStackTrace();
        }

        return auction;
    }

    /**
     * Place a bid and store in database
     */
    public boolean placeBid(String auctionId, Bidder bidder, BigDecimal amount) {
        if (auctionId == null || bidder == null || amount == null) throw new IllegalArgumentException();
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException();
        }
        boolean success = auction.placeBid(bidder, amount);

        if (success) {
            // Store bid in database
            try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO bids (auction_id, bidder_id, bid_amount) VALUES ((SELECT id FROM auctions WHERE id = ?), ?, ?)")) {

                pstmt.setString(1, auctionId);
                pstmt.setInt(2, getUserIdFromDatabase(bidder.getUsername()));
                pstmt.setBigDecimal(3, amount);

                pstmt.executeUpdate();

                // Update current auction price
                updateAuctionPrice(auctionId, amount);
            } catch (SQLException e) {
                System.err.println("Error storing bid: " + e.getMessage());
            }
        }

        return success;
    }

    /**
     * Get auction by ID
     */
    public Auction getAuction(String id) {
        if (id == null) return null;
        return auctions.get(id);
    }

    /**
     * Register autobid and store in database
     */
    public void registerAutoBid(String auctionId, Bidder bidder, BigDecimal maxBid) {
        if (auctionId == null || bidder == null || maxBid == null) {
            throw new IllegalArgumentException();
        }
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException();
        }
        auction.registerAutoBid(bidder, maxBid);

        // Store in database
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO auto_bids (auction_id, bidder_id, max_bid_amount) VALUES ((SELECT id FROM auctions WHERE id = ?), ?, ?)")) {

            pstmt.setString(1, auctionId);
            pstmt.setInt(2, getUserIdFromDatabase(bidder.getUsername()));
            pstmt.setBigDecimal(3, maxBid);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error registering autobid: " + e.getMessage());
        }
    }

    /**
     * Get auctions by status
     */
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        List<Auction> allAuctions = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getStatus() == status) {
                allAuctions.add(a);
            }
        }
        return allAuctions;
    }

    /**
     * Get all auctions and update status if needed
     */
    public List<Auction> getAllAuctions() {
        Instant now = Instant.now();
        for (Auction auction : auctions.values()) {
            if (auction.getStatus() == AuctionStatus.OPEN && !now.isBefore(auction.getStartTime()) && now.isBefore(auction.getEndTime())) {
                try {
                    java.lang.reflect.Field statusField = Auction.class.getDeclaredField("status");
                    statusField.setAccessible(true);
                    statusField.set(auction, AuctionStatus.RUNNING);
                } catch (Exception ignored) {}
            } else if (auction.getStatus() == AuctionStatus.RUNNING && !now.isBefore(auction.getEndTime())) {
                try {
                    java.lang.reflect.Field statusField = Auction.class.getDeclaredField("status");
                    statusField.setAccessible(true);
                    statusField.set(auction, AuctionStatus.FINISHED);
                } catch (Exception ignored) {}
            }
        }
        return new ArrayList<>(auctions.values());
    }

    /**
     * Mark item as paid
     */
    public void itemPaid(String auctionId, Bidder bidder) {
        if (auctionId == null || bidder == null) {
            throw new IllegalArgumentException();
        }
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException();
        }
        auction.itemPaid(bidder);
    }

    /**
     * Get auctions by seller
     */
    public List<Auction> getAuctionsBySeller(String sellerUsername) {
        List<Auction> sellerAuctions = new ArrayList<>();
        for (Auction auction : auctions.values()) {
            if (auction.getSeller() != null && auction.getSeller().getUsername().equals(sellerUsername)) {
                sellerAuctions.add(auction);
            }
        }
        return sellerAuctions;
    }

    /**
     * Helper method to save item to database
     */
    private int saveItemToDatabase(Item item, Seller seller) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO items (seller_id, name, description, category, status) VALUES (?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            int sellerId = getUserIdFromDatabase(seller.getUsername());
            pstmt.setInt(1, sellerId);
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());

            // Get category from item class type
            String category = item.getClass().getSimpleName().toUpperCase();
            if (category.equals("COLLECTIBLE")) category = "COLLECTIBLES";
            else if (category.equals("ELECTRONIC")) category = "ELECTRONICS";
            else if (category.equals("ART")) category = "ARTS";
            else if (category.equals("VEHICLE")) category = "VEHICLES";
            else if (category.equals("FASHION")) category = "FASHIONS";

            pstmt.setString(4, category);
            pstmt.setString(5, "AVAILABLE"); // Default status

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving item to database: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Helper method to get user ID from database by username
     */
    private int getUserIdFromDatabase(String username) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user ID: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Update auction current price
     */
    private void updateAuctionPrice(String auctionId, BigDecimal newPrice) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE auctions SET current_price = ? WHERE id = ?")) {

            pstmt.setBigDecimal(1, newPrice);
            pstmt.setString(2, auctionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating auction price: " + e.getMessage());
        }
    }
}