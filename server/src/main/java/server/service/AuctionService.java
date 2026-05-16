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
    private final WalletService walletService;
    private final UserService userService;
    private static final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2);
    private static final Map<String, Integer> userIdCache = new ConcurrentHashMap<>(); // Cache để tránh lặp lại query DB

    public AuctionService(UserService userService, WalletService walletService) {
        this.userService = userService;
        this.walletService = walletService;
        // Tải tất cả các phiên đấu giá từ cơ sở dữ liệu khi server khởi động
        initializeAuctionsFromDatabase();
    }

    // Tải tất cả các phiên đấu giá từ cơ sở dữ liệu khi server khởi động
    private void initializeAuctionsFromDatabase() {
        System.out.println("Loading auctions from database...");
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, seller_id, base_price, current_price, auction_status, " +
                        "start_time, end_time, auction_id " +
                        "FROM items WHERE auction_id IS NOT NULL AND auction_status IN ('OPEN', 'RUNNING')")) {

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

                    auction.startScheduler();

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
                "SELECT name, description, category, image_url, base_price, " +
                        "brand, item_status, model_year, km_travel, artist, year_created, is_original, seller_id, min_increment "
                        +
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
                if (!(sellerUser instanceof Seller))
                    return null;
                Seller seller = (Seller) sellerUser;

                Item item = null;
                if ("ELECTRONICS".equals(category)) {
                    String brand = rs.getString("brand");
                    String statusStr = rs.getString("item_status");
                    shared.enums.ItemStatus itemStatus = shared.enums.ItemStatus
                            .valueOf(statusStr != null ? statusStr : "NEW");
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
                    shared.enums.ItemStatus itemStatus = shared.enums.ItemStatus
                            .valueOf(statusStr != null ? statusStr : "NEW");
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
                        throw new RuntimeException("min_increment is null for item " + itemId);
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
                "SELECT ab.bidder_id, ab.max_bid_amount, ab.created_at FROM auto_bids ab " + "WHERE ab.auction_id = ?")) {

            pstmt.setString(1, auction.getId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                // Chỉ nạp autobid nếu phiên đấu giá vẫn đang mở hoặc đang diễn ra
                if (auction.getStatus() != shared.enums.AuctionStatus.OPEN &&
                        auction.getStatus() != shared.enums.AuctionStatus.RUNNING) {
                    continue;
                }

                int bidderIdVal = rs.getInt("bidder_id");
                BigDecimal maxBid = rs.getBigDecimal("max_bid_amount");
                Timestamp createdAt = rs.getTimestamp("created_at");
                Instant timeStamp = (createdAt != null) ? createdAt.toInstant() : Instant.now();
                
                String bidderUsername = getUsernameFromId(conn, bidderIdVal);
                User bidderUser = userService.getUser(bidderUsername);

                if (bidderUser instanceof Bidder) {
                    try {
                        auction.registerAutoBid((Bidder) bidderUser, maxBid, timeStamp);
                    } catch (Exception e) {
                        System.err.println(" [RESTORE] Skipped 1 autobid for auction " + auction.getId() + ": " + e.getMessage());
                    }
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
        // Đăng ký finish callback để service có thể tự động thanh toán khi phiên đấu
        // giá kết thúc
        auction.setFinishCallback(a -> finalizeAuction(a));
        auction.setBanChecker(username -> userService.isUserBanned(username));
        auction.startScheduler();
        auctions.put(id, auction);
        System.out.println(" [MEMORY] Added new auction to map. Current total in memory: " + auctions.size());

        int dbId = saveItemAndAuctionToDatabase(item, seller, startPrice, startTime, endTime, id); // Lưu item và
                                                                                                   // auction vào
                                                                                                   // database
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

        // Restrict bidder to 1 active auction at a time
        for (Auction a : auctions.values()) {
            if (!a.getId().equals(auctionId) &&
                    (a.getStatus() == AuctionStatus.RUNNING || a.getStatus() == AuctionStatus.OPEN)) {
                if (a.hasBidder(bidder.getUsername())) {
                    throw new IllegalStateException("You can only participate in 1 auction at a time!");
                }
            }
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

        // Restrict bidder to 1 active auction at a time
        for (Auction a : auctions.values()) {
            if (!a.getId().equals(auctionId) &&
                    (a.getStatus() == AuctionStatus.RUNNING || a.getStatus() == AuctionStatus.OPEN)) {
                if (a.hasBidder(bidder.getUsername())) {
                    throw new IllegalStateException("You can only participate in 1 auction at a time!");
                }
            }
        }

        // Đảm bảo bidder có đủ số dư cho auto bid
        BigDecimal balance = walletService.getWalletBalance(bidder.getUsername());
        if (balance == null || balance.compareTo(maxBid) < 0) {
            throw new IllegalStateException("Not enough balance to register auto-bid.");
        }

        auction.registerAutoBid(bidder, maxBid);

        // Lưu vào database (Xóa cái cũ trước để tránh bị trùng lặp khi restart server)
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Xóa cấu hình autobid cũ của user này cho auction này
                try (PreparedStatement deletePstmt = conn.prepareStatement(
                        "DELETE FROM auto_bids WHERE auction_id = ? AND bidder_id = ?")) {
                    deletePstmt.setString(1, auctionId);
                    deletePstmt.setInt(2, getUserIdFromDatabase(bidder.getUsername()));
                    deletePstmt.executeUpdate();
                }

                // 2. Thêm cấu hình mới
                try (PreparedStatement insertPstmt = conn.prepareStatement(
                        "INSERT INTO auto_bids (auction_id, bidder_id, max_bid_amount, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {
                    insertPstmt.setString(1, auctionId);
                    insertPstmt.setInt(2, getUserIdFromDatabase(bidder.getUsername()));
                    insertPstmt.setBigDecimal(3, maxBid);
                    insertPstmt.executeUpdate();
                }

                conn.commit();
                System.out.println("Auto-bid updated in database for user: " + bidder.getUsername());
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error updating autobid in database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Lấy phiên đấu giá theo trạng thái
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        syncWithDatabase(); // Luôn đồng bộ với DB trước khi trả về
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
        // System.out.println(" [QUERY] Sync completed. Client requested all auctions.
        // Total: " + auctions.size());
        for (Auction auction : auctions.values()) {
            auction.updateStatus();
        }

        return new ArrayList<>(auctions.values());
    }

    // Đồng bộ hóa dữ liệu từ Database vào RAM.
    // Giúp nhiều Server chạy song song vẫn nhìn thấy dữ liệu của nhau.
    private void syncWithDatabase() {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT id, current_price, auction_status, auction_id FROM items WHERE auction_id IS NOT NULL AND auction_status IN ('OPEN', 'RUNNING')")) {

            java.util.Set<String> dbAuctionIds = new java.util.HashSet<>();
            while (rs.next()) {
                String auctionId = rs.getString("auction_id");
                dbAuctionIds.add(auctionId);
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

            // Xóa các auction khỏi RAM nếu nó đã bị xóa ở Database (bởi 1 máy khác) Chỉ xóa
            // nếu item đó đã từng được lưu vào DB (getDbId() > 0)
            auctions.entrySet().removeIf(
                    entry -> !dbAuctionIds.contains(entry.getKey()) && entry.getValue().getItem().getDbId() > 0);

        } catch (SQLException e) {
            System.err.println("Error during DB sync: " + e.getMessage());
        }
    }

    private void loadSingleAuctionFromDB(Connection conn, String targetAuctionId) {
        String sql = "SELECT id, seller_id, base_price, current_price, auction_status, " +
                "start_time, end_time, auction_id " + "FROM items WHERE auction_id = ? AND auction_status IN ('OPEN', 'RUNNING')";
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
                        Auction auction = new Auction(targetAuctionId, item, startPrice, (Seller) seller, startTime,
                                endTime);
                        auction.setFinishCallback(a -> finalizeAuction(a));
                        auction.setBanChecker(username -> userService.isUserBanned(username));

                        if (currentPrice != null && currentPrice.compareTo(startPrice) > 0) {
                            auction.setCurrentPriceForDBRestore(currentPrice);
                        }

                        auction.setStatusForDBRestore(AuctionStatus.valueOf(statusStr));

                        // Load bid history & auto-bids
                        loadBidHistoryForAuction(conn, auction, userService);
                        loadAutoBidsForAuction(conn, auction, userService);

                        auction.startScheduler();

                        auctions.put(targetAuctionId, auction);
                        System.out.println("[SYNC] Loaded new auction from DB: " + targetAuctionId);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error loading single auction during sync: " + e.getMessage());
        }
    }

    // Thanh toán tiền đấu giá khi phiên đấu giá kết thúc. Được gọi bởi
    // Auction.finishCallback.
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
                        System.err.println("Không thể thanh toán tiền cho phiên đấu giá " + auction.getId() + ": "
                                + finalizeError);
                    }

                } else {
                    // Không có người thắng: chỉ cập nhật trạng thái phiên đấu giá thành FINISHED
                    // trong DB
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
        syncWithDatabase(); // Luôn đồng bộ với DB trước khi trả về
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
        auctions.remove(auctionId);

        // Giải phóng toàn bộ tiền đang bị đóng băng (hold) của những người đã đặt giá
        walletService.releaseAllHoldsForAuction(auctionId);

        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Xóa lịch sử đặt thầu
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM bids WHERE auction_id = ?")) {
                    pstmt.setString(1, auctionId);
                    pstmt.executeUpdate();
                }

                // 2. Xóa cấu hình autobid
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM auto_bids WHERE auction_id = ?")) {
                    pstmt.setString(1, auctionId);
                    pstmt.executeUpdate();
                }

                // 3. Xóa chính đấu giá đó (bao gồm cả item vì dùng chung bảng items)
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM items WHERE auction_id = ?")) {
                    pstmt.setString(1, auctionId);
                    pstmt.executeUpdate();
                }

                conn.commit();
                System.out.println(" [PERMANENT DELETE] Auction " + auctionId
                        + " and all related data (bids, auto-bids, holds) have been removed.");

                try {
                    com.google.gson.Gson gson = shared.utils.GsonUtils.createGson();
                    shared.network.Response resp = new shared.network.Response("AUCTION_UPDATED", auctionId);
                    server.controller.ClientHandler.broadcast(gson.toJson(resp));
                } catch (Exception ignored) {
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.err.println("Error deleting auction from database: " + e.getMessage());
            return "Database error while deleting auction.";
        }

        return null;
    }

    // Phương thức trợ giúp để lưu mặt hàng vào cơ sở dữ liệu với thông tin giá cả
    private int saveItemAndAuctionToDatabase(Item item, Seller seller, BigDecimal startPrice, Instant startTime,
            Instant endTime, String auctionId) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO items (seller_id, name, description, category, status, " +
                                "base_price, current_price, seller_name, " +
                                "brand, item_status, model_year, km_travel, artist, year_created, is_original, image_url, min_increment, "
                                +
                                "auction_id, auction_status, start_time, end_time) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {

            int sellerId = getUserIdFromDatabase(seller.getUsername());
            pstmt.setInt(1, sellerId);
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());

            String category = item.getClass().getSimpleName().toUpperCase() + "S"; // Get category from item class type

            pstmt.setString(4, category);
            pstmt.setString(5, "AVAILABLE"); // Default status
            // Set pricing information
            pstmt.setBigDecimal(6, startPrice); // base_price
            pstmt.setBigDecimal(7, startPrice); // current_price
            pstmt.setString(8, seller.getUsername()); // seller_name

            // Initialize all item-specific fields to null first
            pstmt.setNull(9, java.sql.Types.VARCHAR); // brand
            pstmt.setNull(10, java.sql.Types.VARCHAR); // item_status
            pstmt.setNull(11, java.sql.Types.INTEGER); // model_year
            pstmt.setNull(12, java.sql.Types.INTEGER); // km_travel
            pstmt.setNull(13, java.sql.Types.VARCHAR); // artist
            pstmt.setNull(14, java.sql.Types.INTEGER); // year_created
            pstmt.setNull(15, java.sql.Types.BOOLEAN); // is_original
            pstmt.setString(16, item.getImageUrl()); // image_url
            pstmt.setBigDecimal(17, item.getMinIncrement()); // min_increment

            // Set auction information
            pstmt.setString(18, auctionId);
            pstmt.setString(19, AuctionStatus.OPEN.name());
            pstmt.setTimestamp(20, Timestamp.from(startTime));
            pstmt.setTimestamp(21, Timestamp.from(endTime));

            // Set item-specific fields based on type
            if (item instanceof shared.models.Electronic electronic) {
                pstmt.setString(9, electronic.getBrand()); // brand
                pstmt.setString(10, electronic.getStatus().name()); // item_status
            } else if (item instanceof shared.models.Vehicle vehicle) {
                pstmt.setString(9, vehicle.getBrand()); // brand
                pstmt.setInt(11, vehicle.getModel()); // model_year
                pstmt.setInt(12, vehicle.getKMTravel()); // km_travel
            } else if (item instanceof shared.models.Art art) {
                pstmt.setString(13, art.getArtist()); // artist
                pstmt.setInt(14, art.getYearCreated()); // year_created
                pstmt.setBoolean(15, art.getIsOriginal()); // is_original
            } else if (item instanceof shared.models.Fashion fashion) {
                pstmt.setString(9, fashion.getBrand()); // brand
                pstmt.setString(10, fashion.getStatus().name()); // item_status
            } else if (item instanceof shared.models.Collectible collectible) {
                pstmt.setInt(14, collectible.getYearCreated()); // year_created
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

    // Helper method to get user ID from database by username (WITH CACHING)
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

    // Update auction current price (ASYNC to avoid blocking)
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

    // Dừng tất cả các phiên đấu giá của một seller cụ thể (thường dùng khi ban user)
    public void terminateAllAuctionsBySeller(String sellerUsername, String adminUsername) {
        System.out.println("[AUCTION SERVICE] Terminating all auctions for seller: " + sellerUsername);
        List<String> auctionIdsToTerminate = new ArrayList<>();
        
        for (Auction auction : auctions.values()) {
            if (auction.getSeller() != null && auction.getSeller().getUsername().equals(sellerUsername)) {
                auctionIdsToTerminate.add(auction.getId());
            }
        }
        
        for (String id : auctionIdsToTerminate) {
            terminateAuction(id, adminUsername);
        }
    }

    // Thêm phương thức này để lớp test có thể gọi
    public void clearCache() {
        userIdCache.clear();
    }
}