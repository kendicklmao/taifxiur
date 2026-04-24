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
import server.service.WalletService;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import shared.utils.GsonUtils;
import shared.network.Response;
import server.controller.ClientHandler;

public class AuctionService {
    private final ConcurrentHashMap<String, Auction> auctions = new ConcurrentHashMap<>(); //lưu các cuộc giao dịch
    private static final WalletService walletService = new WalletService();

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
        // Ensure auction start time is at least 1 minute in the future from now.
        Instant now = Instant.now();
        if (startTime == null || startTime.isBefore(now)) {
            // If caller did not provide a start time or provided a time too soon,
            // set the auction to start 1 minute from now.
        }
        // Ensure endTime is after startTime
        if (endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime and cannot be null");
        }

        String id = UUID.randomUUID().toString();
        Auction auction = new Auction(id, item, startPrice, seller, startTime, endTime);
        // Register finish callback so the service finalizes payment automatically when auction finishes
        auction.setFinishCallback(a -> finalizeAuction(a));
        auctions.put(id, auction);

        // Store item in database with pricing information
        int itemId = saveItemToDatabase(item, seller, startPrice);

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
        // Ensure bidder has enough available balance
        BigDecimal available = walletService.getAvailableBalance(bidder.getUsername());
        if (available == null || available.compareTo(amount) < 0) {
            return false; // insufficient funds
        }

        Bidder previousHighest = auction.getHighestBidder();
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
                    // When auction becomes finished, try to finalize payment automatically
                    try {
                        Bidder winner = auction.getHighestBidder();
                        if (winner != null) {
                            String auctionId = auction.getId();
                            String winnerUsername = winner.getUsername();
                            String sellerUsername = auction.getSeller().getUsername();
                            BigDecimal price = auction.getCurrentPrice();
                            String finalizeError = walletService.finalizePaymentForWinner(auctionId, winnerUsername, sellerUsername, price);
                            if (finalizeError == null) {
                                // update auction status in DB to PAID
                                try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                                     PreparedStatement pstmt = conn.prepareStatement(
                                             "UPDATE auctions SET status = ? WHERE id = ?")) {
                                    pstmt.setString(1, AuctionStatus.PAID.name());
                                    pstmt.setString(2, auctionId);
                                    pstmt.executeUpdate();
                                } catch (SQLException e) {
                                    System.err.println("Error updating auction status to PAID: " + e.getMessage());
                                }
                            } else {
                                System.err.println("Failed to finalize payment for auction " + auction.getId() + ": " + finalizeError);
                            }
                        } else {
                            // No winner -> release all holds
                            walletService.releaseAllHoldsForAuction(auction.getId());
                        }
                    } catch (Exception e) {
                        System.err.println("Error finalizing auction payment: " + e.getMessage());
                    }
                } catch (Exception ignored) {}
            }
        }
        return new ArrayList<>(auctions.values());
    }

    /**
     * Finalize auction payment when auction finishes. Called by Auction.finishCallback.
     */
    private void finalizeAuction(Auction auction) {
        if (auction == null) return;
        try {
            Bidder winner = auction.getHighestBidder();
            if (winner != null) {
                String auctionId = auction.getId();
                String winnerUsername = winner.getUsername();
                String sellerUsername = auction.getSeller().getUsername();
                BigDecimal price = auction.getCurrentPrice();
                String finalizeError = walletService.finalizePaymentForWinner(auctionId, winnerUsername, sellerUsername, price);
                if (finalizeError == null) {
                    // update auction status in DB to PAID
                    try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                         PreparedStatement pstmt = conn.prepareStatement("UPDATE auctions SET status = ? WHERE id = ?")) {
                        pstmt.setString(1, AuctionStatus.PAID.name());
                        pstmt.setString(2, auctionId);
                        pstmt.executeUpdate();
                    } catch (SQLException e) {
                        System.err.println("Error updating auction status to PAID: " + e.getMessage());
                    }
                    // Notify clients
                    try {
                        Gson gson = GsonUtils.createGson();
                        Response resp = new Response("AUCTION_FINISHED", auction.getId());
                        ClientHandler.broadcast(gson.toJson(resp));
                    } catch (Exception ignored) {}
                } else {
                    System.err.println("Failed to finalize payment for auction " + auction.getId() + ": " + finalizeError);
                }
            } else {
                // No winner, just notify clients
                try {
                    Gson gson = GsonUtils.createGson();
                    Response resp = new Response("AUCTION_FINISHED", auction.getId());
                    ClientHandler.broadcast(gson.toJson(resp));
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("Error finalizing auction payment: " + e.getMessage());
        }
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
     * Helper method to save item to database with pricing information
     */
    private int saveItemToDatabase(Item item, Seller seller, BigDecimal startPrice) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO items (seller_id, name, description, category, status, item_type, " +
                     "base_price, current_price, legit_check, seller_name, " +
                     "brand, item_status, model_year, km_travel, artist, year_created, is_original, image_url) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
            pstmt.setString(6, item.getClass().getSimpleName()); // item_type

            // Set pricing information
            pstmt.setBigDecimal(7, startPrice); // base_price
            pstmt.setBigDecimal(8, startPrice); // current_price
            pstmt.setBoolean(9, false); // legit_check
            pstmt.setString(10, seller.getUsername()); // seller_name

            // Initialize all item-specific fields to null first
            pstmt.setNull(11, java.sql.Types.VARCHAR); // brand
            pstmt.setNull(12, java.sql.Types.VARCHAR); // item_status
            pstmt.setNull(13, java.sql.Types.INTEGER); // model_year
            pstmt.setNull(14, java.sql.Types.INTEGER); // km_travel
            pstmt.setNull(15, java.sql.Types.VARCHAR); // artist
            pstmt.setNull(16, java.sql.Types.INTEGER); // year_created
            pstmt.setNull(17, java.sql.Types.BOOLEAN); // is_original
            pstmt.setString(18, item.getImageUrl()); // image_url

            // Set item-specific fields based on type
            if (item instanceof shared.models.Electronic electronic) {
                pstmt.setString(11, electronic.getBrand()); // brand
                pstmt.setString(12, electronic.getStatus().name()); // item_status
            } else if (item instanceof shared.models.Vehicle vehicle) {
                pstmt.setString(11, vehicle.getBrand()); // brand
                pstmt.setInt(13, vehicle.getModel()); // model_year
                pstmt.setInt(14, vehicle.getKMTravel()); // km_travel
            } else if (item instanceof shared.models.Art art) {
                pstmt.setString(15, art.getArtist()); // artist
                pstmt.setInt(16, art.getYearCreated()); // year_created
                pstmt.setBoolean(17, art.getIsOriginal()); // is_original
            } else if (item instanceof shared.models.Fashion fashion) {
                pstmt.setString(11, fashion.getBrand()); // brand
                pstmt.setString(12, fashion.getStatus().name()); // item_status
            } else if (item instanceof shared.models.Collectible collectible) {
                pstmt.setInt(16, collectible.getYearCreated()); // year_created
            }

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

