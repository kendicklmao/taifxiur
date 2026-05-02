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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;
import shared.utils.GsonUtils;
import shared.network.Response;
import server.controller.ClientHandler;

public class AuctionService {
    private static final ConcurrentHashMap<String, Auction> auctions = new ConcurrentHashMap<>();
    private static final WalletService walletService = new WalletService();
    private static final UserService userService = new UserService();
    private static final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2);
    // Cache để tránh lặp lại query DB
    private static final Map<String, Integer> userIdCache = new ConcurrentHashMap<>();

    public AuctionService() {
        // Tải tất cả các phiên đấu giá từ cơ sở dữ liệu khi server khởi động
        initializeAuctionsFromDatabase();
    }

    // Tải tất cả các phiên đấu giá từ cơ sở dữ liệu khi server khởi động
    private void initializeAuctionsFromDatabase() {
        System.out.println("Loading auctions from database...");
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id, seller_id, base_price, current_price, auction_status, " +
                             "start_time, end_time, winner_id, auction_id " +
                             "FROM items WHERE auction_id IS NOT NULL")) {

            while (rs.next()) {
                String auctionId = "Unknown";
                try {
                    auctionId = rs.getString("auction_id");
                    int itemId = rs.getInt("id");
                    int sellerId = rs.getInt("seller_id");
                    BigDecimal startPrice = rs.getBigDecimal("base_price");
                    BigDecimal currentPrice = rs.getBigDecimal("current_price");
                    String statusStr = rs.getString("auction_status");
                    Instant startTime = rs.getTimestamp("start_time").toInstant();
                    Instant endTime = rs.getTimestamp("end_time").toInstant();
                    Integer winnerId = rs.getObject("winner_id") != null ? rs.getInt("winner_id") : null;

                    // Load item
                    Item item = loadItemFromDatabase(conn, itemId);
                    if (item == null) {
                        System.err.println("Could not load item " + itemId + " for auction " + auctionId);
                        continue;
                    }

                    // Load seller
                    User seller = userService.getUser(getUsernameFromId(conn, sellerId));
                    if (seller == null || !(seller instanceof Seller)) {
                        System.err.println("Could not load seller for auction " + auctionId);
                        continue;
                    }

                    // Tạo auction và khôi phục state
                    Auction auction = new Auction(auctionId, item, startPrice, (Seller) seller, startTime, endTime);
                    auction.setFinishCallback(a -> finalizeAuction(a));

                    // Restore current price if different from start price
                    if (currentPrice != null && currentPrice.compareTo(startPrice) > 0) {
                        auction.setCurrentPriceForDBRestore(currentPrice);
                    }

                    // Restore status
                    auction.setStatusForDBRestore(AuctionStatus.valueOf(statusStr));

                    // Load and restore bid history
                    loadBidHistoryForAuction(conn, auction, userService);
                    
                    // Load and restore auto-bids
                    loadAutoBidsForAuction(conn, auction, userService);

                    auctions.put(auctionId, auction);
                    System.out.println("Loaded auction: " + auctionId + " (status: " + statusStr + ")");
                } catch (IllegalArgumentException e) {
                    System.out.println("Skipped loading auto-bids for auction " + auctionId + ": " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Error loading auction " + auctionId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            System.out.println("✓ Auction loading complete. Total auctions loaded: " + auctions.size());
        } catch (SQLException e) {
            System.err.println("Error initializing auctions from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Tải item từ database
    private Item loadItemFromDatabase(Connection conn, int itemId) {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT name, description, category, item_type, image_url, base_price, " +
                "brand, item_status, model_year, km_travel, artist, year_created, is_original, seller_id, min_increment " +
                "FROM items WHERE id = ?")) {

            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("name");
                String description = rs.getString("description");
                String category = rs.getString("category");
                int sellerId = rs.getInt("seller_id");
                String imageUrl = rs.getString("image_url");
                BigDecimal minIncrement = rs.getBigDecimal("min_increment");
                BigDecimal basePrice = rs.getBigDecimal("base_price");

                User sellerUser = userService.getUser(getUsernameFromId(conn, sellerId));
                if (!(sellerUser instanceof Seller)) return null;
                Seller seller = (Seller) sellerUser;

                Item item = null;
                if ("ELECTRONICS".equals(category)) {
                    String brand = rs.getString("brand");
                    String statusStr = rs.getString("item_status");
                    shared.enums.ItemStatus itemStatus = shared.enums.ItemStatus.valueOf(statusStr != null ? statusStr : "NEW");
                    item = new shared.models.Electronic(name, description, seller, basePrice, brand, itemStatus);
                } else if ("VEHICLES".equals(category)) {
                    String brand = rs.getString("brand");
                    int model = rs.getInt("model_year");
                    int km = rs.getInt("km_travel");
                    item = new shared.models.Vehicle(name, description, seller, basePrice, brand, model, km);
                } else if ("ARTS".equals(category)) {
                    String artist = rs.getString("artist");
                    int year = rs.getInt("year_created");
                    boolean isOriginal = rs.getBoolean("is_original");
                    item = new shared.models.Art(name, description, seller, basePrice, artist, year, isOriginal);
                } else if ("FASHIONS".equals(category)) {
                    String brand = rs.getString("brand");
                    String statusStr = rs.getString("item_status");
                    shared.enums.ItemStatus itemStatus = shared.enums.ItemStatus.valueOf(statusStr != null ? statusStr : "NEW");
                    item = new shared.models.Fashion(name, description, seller, basePrice, brand, itemStatus);
                } else if ("COLLECTIBLES".equals(category)) {
                    int year = rs.getInt("year_created");
                    item = new shared.models.Collectible(name, description, seller, basePrice, year);
                }

                if (item != null) {
                    item.setImageUrl(imageUrl);
                    if (minIncrement != null) {
                        item.setMinIncrement(minIncrement);
                    } else {
                        // Mặc định nếu min_increment bị thiếu
                        item.setMinIncrement(new BigDecimal("10000"));
                    }
                    item.setDbId(itemId);
                }
                return item;
            }
        } catch (SQLException e) {
            System.err.println("Error loading item from database: " + e.getMessage());
        }
        return null;
    }

    // Tải username từ user ID
    private String getUsernameFromId(Connection conn, int userId) {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT username FROM users WHERE id = ?")) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching username from ID: " + e.getMessage());
        }
        return null;
    }

    // Load lịch sử đấu giá cho một phiên đấu giá
    private void loadBidHistoryForAuction(Connection conn, Auction auction, UserService userService) {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT b.bidder_id, b.bid_amount, b.bid_time FROM bids b " +
                "WHERE b.auction_id = ? ORDER BY b.bid_time ASC")) {

            pstmt.setString(1, auction.getId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int bidderId = rs.getInt("bidder_id");
                BigDecimal bidAmount = rs.getBigDecimal("bid_amount");
                String bidderUsername = getUsernameFromId(conn, bidderId);
                User bidderUser = userService.getUser(bidderUsername);

                if (bidderUser instanceof Bidder) {
                    auction.restoreBid((Bidder) bidderUser, bidAmount, rs.getTimestamp("bid_time").toInstant());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading bid history: " + e.getMessage());
        }
    }

    // Tải auto-bids cho một phiên đấu giá
    private void loadAutoBidsForAuction(Connection conn, Auction auction, UserService userService) {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT ab.bidder_id, ab.max_bid_amount FROM auto_bids ab " +
                "WHERE ab.auction_id = ?")) {

            pstmt.setString(1, auction.getId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int bidderId = rs.getInt("bidder_id");
                BigDecimal maxBid = rs.getBigDecimal("max_bid_amount");
                String bidderUsername = getUsernameFromId(conn, bidderId);
                User bidderUser = userService.getUser(bidderUsername);

                if (bidderUser instanceof Bidder) {
                    auction.registerAutoBid((Bidder) bidderUser, maxBid);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading auto-bids: " + e.getMessage());
        }
    }

    // Tạo phiên đấu giá và lưu vào database
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
        // Đảm bảo thời gian bắt đầu đấu giá hợp lệ
        Instant now = Instant.now();
        if (startTime == null || startTime.isBefore(now)) {
            startTime = now;
        }
        // Đảm bảo thời gian kết thúc đấu giá hợp lệ
        if (endTime == null || !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime and cannot be null");
        }

        String id = UUID.randomUUID().toString();
        Auction auction = new Auction(id, item, startPrice, seller, startTime, endTime);
        // Đăng ký finish callback để service có thể tự động thanh toán khi phiên đấu giá kết thúc
        auction.setFinishCallback(a -> finalizeAuction(a));
        auction.setBanChecker(username -> userService.isUserBanned(username));
        auctions.put(id, auction);
        System.out.println("✅ [MEMORY] Added new auction to map. Current total in memory: " + auctions.size());

        // Lưu item và auction vào database
        int dbId = saveItemAndAuctionToDatabase(item, seller, startPrice, startTime, endTime, id);
        if (dbId == -1) {
            auctions.remove(id); // remove from in-memory map if DB save failed
            throw new RuntimeException("Failed to save auction to database.");
        }

        return auction;
    }

    // Đặt giá và lưu vào database
    public boolean placeBid(String auctionId, Bidder bidder, BigDecimal amount) {
        if (auctionId == null || bidder == null || amount == null)
            throw new IllegalArgumentException();
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException();
        }
        // Đảm bảo bidder có đủ số dư (không có hệ thống hold)
        BigDecimal balance = walletService.getWalletBalance(bidder.getUsername());
        if (balance == null || balance.compareTo(amount) < 0) {
            return false; // Số dư không đủ
        }

        boolean success = auction.placeBid(bidder, amount);

        if (success) {
            // Lưu bid vào database
            try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(
                            "INSERT INTO bids (auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {

                pstmt.setString(1, auctionId);
                pstmt.setInt(2, getUserIdFromDatabase(bidder.getUsername()));
                pstmt.setBigDecimal(3, amount);

                int result = pstmt.executeUpdate();
                System.out.println("Bid inserted: " + result + " rows affected");

                // Cập nhật giá hiện tại của phiên đấu giá
                updateItemPrice(auction.getItem().getDbId(), amount);
            } catch (SQLException e) {
                System.err.println("Error storing bid: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return success;
    }

    // Lấy phiên đấu giá theo ID
    public Auction getAuction(String id) {
        if (id == null)
            return null;
        return auctions.get(id);
    }

    // Đăng ký autobid và lưu vào database
    public void registerAutoBid(String auctionId, Bidder bidder, BigDecimal maxBid) {
        if (auctionId == null || bidder == null || maxBid == null) {
            throw new IllegalArgumentException();
        }
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException();
        }
        auction.registerAutoBid(bidder, maxBid);

        // Lưu vào database
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO auto_bids (auction_id, bidder_id, max_bid_amount, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {

            pstmt.setString(1, auctionId);
            pstmt.setInt(2, getUserIdFromDatabase(bidder.getUsername()));
            pstmt.setBigDecimal(3, maxBid);

            int result = pstmt.executeUpdate();
            System.out.println("Auto-bid inserted: " + result + " rows affected");
        } catch (SQLException e) {
            System.err.println("Error registering autobid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Lấy phiên đấu giá theo trạng thái
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        List<Auction> allAuctions = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getStatus() == status) {
                allAuctions.add(a);
            }
        }
        return allAuctions;
    }

    // Lấy tất cả phiên đấu giá và cập nhật trạng thái nếu cần
    public List<Auction> getAllAuctions() {
        syncWithDatabase(); // Luôn đồng bộ với DB trước khi trả về cho Client
        System.out.println("🔍 [QUERY] Sync completed. Client requested all auctions. Total: " + auctions.size());
        for (Auction auction : auctions.values()) {
            auction.updateStatus();
        }
        return new ArrayList<>(auctions.values());
    }

    /**
     * Đồng bộ hóa dữ liệu từ Database vào RAM. 
     * Giúp nhiều Server chạy song song vẫn nhìn thấy dữ liệu của nhau.
     */
    private void syncWithDatabase() {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id, current_price, auction_status, auction_id FROM items WHERE auction_id IS NOT NULL")) {

            while (rs.next()) {
                String auctionId = rs.getString("auction_id");
                BigDecimal dbPrice = rs.getBigDecimal("current_price");
                String dbStatus = rs.getString("auction_status");

                if (auctions.containsKey(auctionId)) {
                    // Nếu đã có trong RAM, cập nhật giá và trạng thái mới nhất từ DB
                    Auction auction = auctions.get(auctionId);
                    if (dbPrice != null && dbPrice.compareTo(auction.getCurrentPrice()) > 0) {
                        auction.setCurrentPriceForDBRestore(dbPrice);
                    }
                    if (dbStatus != null) {
                        auction.setStatusForDBRestore(AuctionStatus.valueOf(dbStatus));
                    }
                } else {
                    // Nếu chưa có trong RAM (do Server khác tạo), tiến hành nạp mới hoàn toàn
                    loadSingleAuctionFromDB(conn, auctionId);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during DB sync: " + e.getMessage());
        }
    }

    private void loadSingleAuctionFromDB(Connection conn, String targetAuctionId) {
        String sql = "SELECT id, seller_id, base_price, current_price, auction_status, " +
                     "start_time, end_time, winner_id, auction_id " +
                     "FROM items WHERE auction_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, targetAuctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int itemId = rs.getInt("id");
                    int sellerId = rs.getInt("seller_id");
                    BigDecimal startPrice = rs.getBigDecimal("base_price");
                    BigDecimal currentPrice = rs.getBigDecimal("current_price");
                    String statusStr = rs.getString("auction_status");
                    Instant startTime = rs.getTimestamp("start_time").toInstant();
                    Instant endTime = rs.getTimestamp("end_time").toInstant();

                    Item item = loadItemFromDatabase(conn, itemId);
                    User seller = userService.getUser(getUsernameFromId(conn, sellerId));

                    if (item != null && seller instanceof Seller) {
                        Auction auction = new Auction(targetAuctionId, item, startPrice, (Seller) seller, startTime, endTime);
                        auction.setFinishCallback(a -> finalizeAuction(a));
                        auction.setBanChecker(username -> userService.isUserBanned(username));
                        
                        if (currentPrice != null && currentPrice.compareTo(startPrice) > 0) {
                            auction.setCurrentPriceForDBRestore(currentPrice);
                        }
                        auction.setStatusForDBRestore(AuctionStatus.valueOf(statusStr));
                        
                        // Load bid history & auto-bids
                        loadBidHistoryForAuction(conn, auction, userService);
                        loadAutoBidsForAuction(conn, auction, userService);

                        auctions.put(targetAuctionId, auction);
                        System.out.println("🆕 [SYNC] Loaded new auction from DB: " + targetAuctionId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading single auction during sync: " + e.getMessage());
        }
    }

    // Thanh toán tiền đấu giá khi phiên đấu giá kết thúc. Được gọi bởi Auction.finishCallback.
    // Hoạt động cho cả bid thủ công và bid tự động vì cả hai đều đặt highestBidder.
    // Chạy ASYNCHRONOUSLY trong background thread pool để tránh bị block!
    private void finalizeAuction(Auction auction) {
        // Gửi đến async executor - không chặn thread scheduler
        asyncExecutor.execute(() -> {
            if (auction == null)
                return;
            try {
                Bidder winner = auction.getHighestBidder();
                if (winner != null) {
                    String auctionId = auction.getId();
                    String winnerUsername = winner.getUsername();
                    String sellerUsername = auction.getSeller().getUsername();
                    BigDecimal price = auction.getCurrentPrice();
                    String finalizeError = walletService.finalizePaymentForWinner(auctionId, winnerUsername,
                            sellerUsername, price);
                    if (finalizeError == null) {
                        // Cập nhật trạng thái phiên đấu giá trong database thành PAID
                        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                                PreparedStatement pstmt = conn
                                        .prepareStatement("UPDATE items SET auction_status = ? WHERE auction_id = ?")) {
                            pstmt.setString(1, AuctionStatus.PAID.name());
                            pstmt.setString(2, auctionId);
                            pstmt.executeUpdate();
                        } catch (SQLException e) {
                            System.err.println("Error updating auction status to PAID: " + e.getMessage());
                        }
                        // Thông báo cho client
                        try {
                            Gson gson = GsonUtils.createGson();
                            Response resp = new Response("AUCTION_PAID", auction.getId());
                            ClientHandler.broadcast(gson.toJson(resp));
                        } catch (Exception ignored) {
                        }
                    } else {
                        System.err.println("Không thể thanh toán tiền cho phiên đấu giá " + auction.getId() + ": " + finalizeError);
                    }
                } else {
                    // Không có người thắng: chỉ cập nhật trạng thái phiên đấu giá thành FINISHED trong DB
                    try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                            PreparedStatement pstmt = conn
                                    .prepareStatement("UPDATE items SET auction_status = ? WHERE auction_id = ?")) {
                        pstmt.setString(1, AuctionStatus.FINISHED.name());
                        pstmt.setString(2, auction.getId());
                        pstmt.executeUpdate();
                    } catch (SQLException e) {
                        System.err.println("Error updating auction status to FINISHED: " + e.getMessage());
                    }
                    // Notify clients
                    try {
                        Gson gson = GsonUtils.createGson();
                        Response resp = new Response("AUCTION_FINISHED", auction.getId());
                        ClientHandler.broadcast(gson.toJson(resp));
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                System.err.println("Error finalizing auction payment: " + e.getMessage());
            }
        });
    }

    // Đánh dấu vật phẩm là đã thanh toán
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

    // Lấy phiên đấu giá theo TÊN người bán
    public List<Auction> getAuctionsBySeller(String sellerUsername) {
        List<Auction> sellerAuctions = new ArrayList<>();
        for (Auction auction : auctions.values()) {
            if (auction.getSeller() != null && auction.getSeller().getUsername().equals(sellerUsername)) {
                // Cập nhật status nếu cần
                auction.updateStatus();
                sellerAuctions.add(auction);
            }
        }
        return sellerAuctions;
    }

    public String terminateAuction(String auctionId, String username) {
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            return "Auction not found.";
        }

        User user = userService.getUser(username);
        if (user == null) {
            return "User not found.";
        }

        if (user.getRole() != shared.enums.Role.ADMIN && !auction.getSeller().getUsername().equals(username)) {
            return "You are not authorized to terminate this auction.";
        }

        auction.cancel();
        auctions.remove(auctionId);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE items SET auction_status = ? WHERE auction_id = ?")) {
            pstmt.setString(1, AuctionStatus.CANCELED.name());
            pstmt.setString(2, auctionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating auction status to CANCELLED: " + e.getMessage());
            return "Database error while terminating auction.";
        }

        return null;
    }

    // Phương thức trợ giúp để lưu mặt hàng vào cơ sở dữ liệu với thông tin giá cả
    private int saveItemAndAuctionToDatabase(Item item, Seller seller, BigDecimal startPrice, Instant startTime, Instant endTime, String auctionId) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO items (seller_id, name, description, category, status, item_type, " +
                                "base_price, current_price, legit_check, seller_name, " +
                                "brand, item_status, model_year, km_travel, artist, year_created, is_original, image_url, min_increment, " +
                                "auction_id, auction_status, start_time, end_time) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {

            int sellerId = getUserIdFromDatabase(seller.getUsername());
            pstmt.setInt(1, sellerId);
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());

            // Get category from item class type
            String category = item.getClass().getSimpleName().toUpperCase();
            if (category.equals("COLLECTIBLE"))
                category = "COLLECTIBLES";
            else if (category.equals("ELECTRONIC"))
                category = "ELECTRONICS";
            else if (category.equals("ART"))
                category = "ARTS";
            else if (category.equals("VEHICLE"))
                category = "VEHICLES";
            else if (category.equals("FASHION"))
                category = "FASHIONS";

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
            pstmt.setBigDecimal(19, item.getMinIncrement()); // min_increment

            // Set auction information
            pstmt.setString(20, auctionId);
            pstmt.setString(21, AuctionStatus.OPEN.name());
            pstmt.setTimestamp(22, Timestamp.from(startTime));
            pstmt.setTimestamp(23, Timestamp.from(endTime));

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
                        int generatedId = generatedKeys.getInt(1);
                        item.setDbId(generatedId);
                        return generatedId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving item to database: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Helper method to get user ID from database by username (WITH CACHING)
     */
    private int getUserIdFromDatabase(String username) {
        // Check cache first
        Integer cachedId = userIdCache.get(username);
        if (cachedId != null) {
            return cachedId;
        }

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                userIdCache.put(username, id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user ID: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Update auction current price (ASYNC to avoid blocking)
     */
    private void updateItemPrice(int itemId, BigDecimal newPrice) {
        asyncExecutor.execute(() -> {
            try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(
                            "UPDATE items SET current_price = ? WHERE id = ?")) {

                pstmt.setBigDecimal(1, newPrice);
                pstmt.setInt(2, itemId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Error updating auction price: " + e.getMessage());
            }
        });
    }
}
