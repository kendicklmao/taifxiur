package server.service;

import server.database.DatabaseConfig;
import shared.enums.AuctionStatus;
import shared.enums.Category;
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
    private static final Map<String, Integer> userIdCache = new ConcurrentHashMap<>(); // Cache để tránh lặp lại query
                                                                                       // DB

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

                    Auction auction = new Auction(auctionId, item, startPrice, (Seller) seller, startTime, endTime);
                    auction.setFinishCallback(a -> finalizeAuction(a));
                    auction.setStatusChangeListener(a -> updateAuctionStatusInDatabase(a));
                    auction.setEndTimeChangeListener(a -> updateAuctionEndTimeInDatabase(a));

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

                    // Đăng ký bid listener sau khi hoàn thành khôi phục
                    auction.setBidListener((bidder, price) -> saveBidToDatabase(auction.getId(), bidder, price,
                            auction.getItem().getDbId()));

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

            System.out.println("Auction loading complete. Total auctions loaded: " + auctions.size());
        } catch (SQLException e) {
            System.err.println("Error initializing auctions from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Tải item từ database
    private Item loadItemFromDatabase(Connection conn, int itemId) {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT name, description, category, image_url, base_price, " +
                        "brand, item_status, model_year, km_travel, artist, year_created, is_original, seller_id, min_increment " +
                        "FROM items WHERE id = ?")) {

            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Category category = Category.valueOf(rs.getString("category"));

                int sellerId = rs.getInt("seller_id");
                String imageUrl = rs.getString("image_url");
                BigDecimal minIncrement = rs.getBigDecimal("min_increment");
                BigDecimal basePrice = rs.getBigDecimal("base_price");

                User sellerUser = userService.getUser(getUsernameFromId(conn, sellerId));
                if (!(sellerUser instanceof Seller seller)) {
                    return null;
                }

                ItemFactory factory = ItemFactoryProvider.getFactory(category);

                if (factory == null) {
                    throw new IllegalArgumentException(
                            "Unsupported category: " + category);
                }

                Item item = factory.create(rs, seller, basePrice);

                item.setImageUrl(imageUrl);

                if (minIncrement == null) {
                    throw new RuntimeException(
                            "min_increment is null for item " + itemId);
                }

                item.setMinIncrement(minIncrement);
                item.setDbId(itemId);

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

    // Chỉ tải thêm các thầu mới từ DB
    private void loadNewBidsForAuction(Connection conn, Auction auction, UserService userService, int skipCount) {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT b.bidder_id, b.bid_amount, b.bid_time FROM bids b " +
                        "WHERE b.auction_id = ? ORDER BY b.bid_time ASC")) {

            pstmt.setString(1, auction.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                int index = 0;
                while (rs.next()) {
                    if (index < skipCount) {
                        index++;
                        continue;
                    }
                    int bidderId = rs.getInt("bidder_id");
                    BigDecimal bidAmount = rs.getBigDecimal("bid_amount");
                    String bidderUsername = getUsernameFromId(conn, bidderId);
                    User bidderUser = userService.getUser(bidderUsername);

                    if (bidderUser instanceof Bidder) {
                        auction.restoreBid((Bidder) bidderUser, bidAmount, rs.getTimestamp("bid_time").toInstant());
                    }
                    index++;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error loading new bids: " + e.getMessage());
        }
    }

    // Tải auto-bids cho một phiên đấu giá
    private void loadAutoBidsForAuction(Connection conn, Auction auction, UserService userService) {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT ab.bidder_id, ab.max_bid_amount, ab.created_at FROM auto_bids ab "
                        + "WHERE ab.auction_id = ?")) {

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
                        auction.restoreAutoBid((Bidder) bidderUser, maxBid, timeStamp);
                    } catch (Exception e) {
                        System.err.println(
                                " [RESTORE] Skipped 1 autobid for auction " + auction.getId() + ": " + e.getMessage());
                    }
                }
            }
            // Chạy lại AutoBidService một lần duy nhất sau khi đã khôi phục toàn bộ autobids
            auction.AutoBidService();
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
        auction.setStatusChangeListener(a -> updateAuctionStatusInDatabase(a));
        auction.setEndTimeChangeListener(a -> updateAuctionEndTimeInDatabase(a));
        auction.setBanChecker(username -> userService.isUserBanned(username));
        auction.setBidListener(
                (bidder, price) -> saveBidToDatabase(auction.getId(), bidder, price, auction.getItem().getDbId()));

        int dbId = saveItemAndAuctionToDatabase(item, seller, startPrice, startTime, endTime, id, auction.getStatus()); // Lưu item và
                                                                                                   // auction vào
                                                                                                   // database
        if (dbId == -1) {
            throw new RuntimeException("Failed to save auction to database.");
        }

        auction.startScheduler();
        auctions.put(id, auction);
        System.out.println(" [MEMORY] Added new auction to map. Current total in memory: " + auctions.size());

        return auction;
    }

    // Đặt giá và lưu vào database
    public boolean placeBid(String auctionId, Bidder bidder, BigDecimal amount) {
        if (auctionId == null || bidder == null || amount == null) {
            throw new IllegalArgumentException();
        }
        Auction auction = getAuction(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException();
        }

        // Giới hạn 1 phiên đấu giá cho mỗi bidder
        checkActiveAuctionParticipation(auctionId, bidder.getUsername());

        // Đảm bảo bidder có đủ số dư (không có hệ thống hold)
        BigDecimal balance = walletService.getWalletBalance(bidder.getUsername());
        if (balance == null || balance.compareTo(amount) < 0) {
            return false; // Số dư không đủ
        }

        return auction.placeBid(bidder, amount);
    }

    // Giới hạn 1 phiên đấu giá cho mỗi bidder
    private void checkActiveAuctionParticipation(String auctionId, String username) {
        for (Auction a : auctions.values()) {
            if (!a.getId().equals(auctionId)) {
                // 1. Đang tham gia phiên khác đang chạy hoặc mở
                boolean isActive = (a.getStatus() == AuctionStatus.RUNNING || a.getStatus() == AuctionStatus.OPEN) && a.hasBidder(username);
                
                // 2. Đã thắng phiên cũ nhưng CHƯA trả tiền (trạng thái FINISHED)
                boolean isUnpaidWin = a.getStatus() == AuctionStatus.FINISHED && a.getHighestBidder() != null && a.getHighestBidder().getUsername().equals(username);

                if (isActive) {
                    throw new IllegalStateException("You can only participate in 1 active auction at a time!");
                }
                if (isUnpaidWin) {
                    throw new IllegalStateException("You must pay for your won auction before participating in a new one!");
                }
            }
        }
    }

    // Lấy phiên đấu giá theo ID
    public Auction getAuction(String id) {
        if (id == null) {
            return null;
        }
        syncWithDatabase();
        return auctions.get(id);
    }

    // Đăng ký autobid và lưu vào database
    public void registerAutoBid(String auctionId, Bidder bidder, BigDecimal maxBid) {
        if (auctionId == null || bidder == null || maxBid == null) {
            throw new IllegalArgumentException();
        }

        Auction auction = getAuction(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException();
        }

        // Restrict bidder to 1 active auction at a time
        checkActiveAuctionParticipation(auctionId, bidder.getUsername());

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
        System.out.println(" [QUERY] Sync completed. Client requested all auctions. Total: " + auctions.size());
        for (Auction auction: auctions.values()) {
            auction.updateStatus();
        }
        return new ArrayList<>(auctions.values());
    }

    // Đồng bộ hóa dữ liệu từ Database vào RAM.
    // Giúp nhiều Server chạy song song vẫn nhìn thấy dữ liệu của nhau.
    private void syncWithDatabase() {
        String sql = "SELECT i.auction_id, i.current_price, i.auction_status, i.end_time, " +
                     "       COALESCE(b.bid_count, 0) as db_bid_count, " +
                     "       COALESCE(ab.autobid_sum, 0) as db_autobid_sum, " +
                     "       COALESCE(ab.autobid_count, 0) as db_autobid_count " +
                     "FROM items i " +
                     "LEFT JOIN ( " +
                     "    SELECT auction_id, COUNT(*) as bid_count " +
                     "    FROM bids " +
                     "    GROUP BY auction_id " +
                     ") b ON i.auction_id = b.auction_id " +
                     "LEFT JOIN ( " +
                     "    SELECT auction_id, SUM(max_bid_amount) as autobid_sum, COUNT(*) as autobid_count " +
                     "    FROM auto_bids " +
                     "    GROUP BY auction_id " +
                     ") ab ON i.auction_id = ab.auction_id " +
                     "WHERE i.auction_id IS NOT NULL";

        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            java.util.Set<String> dbAuctionIds = new java.util.HashSet<>();
            while (rs.next()) {
                String auctionId = rs.getString("auction_id");
                dbAuctionIds.add(auctionId);
                BigDecimal dbPrice = rs.getBigDecimal("current_price");
                String dbStatus = rs.getString("auction_status");
                Instant dbEndTime = rs.getTimestamp("end_time").toInstant();
                int dbBidCount = rs.getInt("db_bid_count");
                BigDecimal dbAutoBidSum = rs.getBigDecimal("db_autobid_sum");
                if (dbAutoBidSum == null) {
                    dbAutoBidSum = BigDecimal.ZERO;
                }
                int dbAutoBidCount = rs.getInt("db_autobid_count");

                if (auctions.containsKey(auctionId)) {
                    // Nếu đã có trong RAM, cập nhật giá và trạng thái mới nhất từ DB
                    Auction auction = auctions.get(auctionId);
                    if (dbEndTime != null) {
                        auction.setEndTimeForDBRestore(dbEndTime);
                    }
                    if (dbStatus != null) {
                        auction.setStatusForDBRestore(AuctionStatus.valueOf(dbStatus));
                    }

                    if (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) {
                        int ramBidCount = auction.getBidHistory().size();
                        if (dbBidCount > ramBidCount) {
                            loadNewBidsForAuction(conn, auction, userService, ramBidCount);
                        }
                        if (dbPrice != null && dbPrice.compareTo(auction.getCurrentPrice()) > 0) {
                            auction.setCurrentPriceForDBRestore(dbPrice);
                        }

                        int ramAutoBidCount = auction.getAutoBidsCount();
                        BigDecimal ramAutoBidSum = auction.getAutoBidsSum();
                        if (ramAutoBidSum == null) {
                            ramAutoBidSum = BigDecimal.ZERO;
                        }
                        
                        boolean autoBidsChanged = (ramAutoBidCount != dbAutoBidCount) || 
                            (dbAutoBidSum.compareTo(ramAutoBidSum) != 0);

                        if (autoBidsChanged) {
                            auction.clearAutoBids();
                            loadAutoBidsForAuction(conn, auction, userService);
                        }
                    } else {
                        if (dbPrice != null && dbPrice.compareTo(auction.getCurrentPrice()) != 0) {
                            int ramBidCount = auction.getBidHistory().size();
                            if (dbBidCount > ramBidCount) {
                                loadNewBidsForAuction(conn, auction, userService, ramBidCount);
                            }
                            auction.setCurrentPriceForDBRestore(dbPrice);
                        }
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
                "start_time, end_time, auction_id "
                + "FROM items WHERE auction_id = ?";
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
                        auction.setEndTimeChangeListener(a -> updateAuctionEndTimeInDatabase(a));
                        auction.setBanChecker(username -> userService.isUserBanned(username));

                        if (currentPrice != null && currentPrice.compareTo(startPrice) > 0) {
                            auction.setCurrentPriceForDBRestore(currentPrice);
                        }

                        auction.setStatusForDBRestore(AuctionStatus.valueOf(statusStr));

                        // Load bid history & auto-bids
                        loadBidHistoryForAuction(conn, auction, userService);
                        loadAutoBidsForAuction(conn, auction, userService);

                        // Đăng ký bid listener sau khi hoàn thành khôi phục
                        auction.setBidListener((bidder, price) -> saveBidToDatabase(auction.getId(), bidder, price,
                                auction.getItem().getDbId()));

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
    // Auction.finishCallback
    // Hoạt động cho cả bid thủ công và bid tự động vì cả hai đều đặt highestBidder
    // Chạy ASYNCHRONOUSLY trong background thread pool để tránh bị block
    private void finalizeAuction(Auction auction) {
        // Gửi đến async executor - không chặn thread scheduler
        asyncExecutor.execute(() -> {
            if (auction == null)
                return;
            try {
                // Cập nhật trạng thái phiên đấu giá trong database thành FINISHED
                try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                        PreparedStatement pstmt = conn
                                .prepareStatement("UPDATE items SET auction_status = ? WHERE auction_id = ?")) {
                    pstmt.setString(1, AuctionStatus.FINISHED.name());
                    pstmt.setString(2, auction.getId());
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    System.err.println("Error updating auction status to FINISHED: " + e.getMessage());
                }

                // Thông báo cho client
                try {
                    Gson gson = GsonUtils.createGson();
                    Response resp = new Response("AUCTION_FINISHED", auction.getId());
                    ClientHandler.broadcast(gson.toJson(resp));
                } catch (Exception ignored) {
                }

            } catch (Exception e) {
                System.err.println("Error finalizing auction: " + e.getMessage());
            }

        });
    }

    // Đánh dấu vật phẩm là đã thanh toán (Thanh toán thủ công)
    public void payItem(String auctionId, Bidder bidder) {
        if (auctionId == null || bidder == null) {
            throw new IllegalArgumentException("Invalid auction ID or bidder");
        }

        Auction auction = getAuction(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException("Auction not found");
        }

        // Thực hiện thanh toán trong database
        String finalizeError = walletService.finalizePaymentForWinner(
                auctionId,
                bidder.getUsername(),
                auction.getSeller().getUsername(),
                auction.getCurrentPrice());

        if (finalizeError != null) {
            throw new IllegalStateException("Payment failed: " + finalizeError);
        }

        // Cập nhật trạng thái phiên đấu giá trong database thành PAID
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn
                        .prepareStatement("UPDATE items SET auction_status = ? WHERE auction_id = ?")) {
            pstmt.setString(1, AuctionStatus.PAID.name());
            pstmt.setString(2, auctionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating auction status to PAID: " + e.getMessage());
            throw new RuntimeException("Database error updating status to PAID: " + e.getMessage());
        }

        // Cập nhật trạng thái in-memory
        auction.payItem(bidder);

        // Thông báo cho client
        try {
            Gson gson = GsonUtils.createGson();
            Response resp = new Response("AUCTION_PAID", auction.getId());
            ClientHandler.broadcast(gson.toJson(resp));
        } catch (Exception ignored) {
        }
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
        Auction auction = getAuction(auctionId);
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
            Instant endTime, String auctionId, AuctionStatus status) {
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
            pstmt.setString(19, status.name());
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

    // Lưu lịch sử đặt thầu vào database
    private void saveBidToDatabase(String auctionId, Bidder bidder, BigDecimal amount, int itemId) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Thêm thầu mới
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO bids (auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, CURRENT_TIMESTAMP)")) {
                    pstmt.setString(1, auctionId);
                    pstmt.setInt(2, getUserIdFromDatabase(bidder.getUsername()));
                    pstmt.setBigDecimal(3, amount);
                    pstmt.executeUpdate();
                }

                // 2. Cập nhật giá hiện tại của phiên đấu giá
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE items SET current_price = ? WHERE id = ?")) {
                    pstmt.setBigDecimal(1, amount);
                    pstmt.setInt(2, itemId);
                    pstmt.executeUpdate();
                }

                conn.commit();
                System.out.println("Bid inserted and item price updated successfully in DB.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error storing bid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Cập nhật trạng thái auction vào database
    private void updateAuctionStatusInDatabase(Auction auction) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE items SET auction_status = ? WHERE auction_id = ?")) {

            pstmt.setString(1, auction.getStatus().toString());
            pstmt.setString(2, auction.getId());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[DB-SYNC] Updated auction " + auction.getId() + " status to " + auction.getStatus()
                        + " in database.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating auction status in database: " + e.getMessage());
        }
    }

    private void updateAuctionEndTimeInDatabase(Auction auction) {
        try (Connection conn = DatabaseConfig.getDataSource().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE items SET end_time = ? WHERE auction_id = ?")) {

            pstmt.setTimestamp(1, Timestamp.from(auction.getEndTime()));
            pstmt.setString(2, auction.getId());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[DB-SYNC] Updated auction " + auction.getId() + " end_time to " + auction.getEndTime()
                        + " in database.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating auction end_time in database: " + e.getMessage());
        }
    }

    // Dừng tất cả các phiên đấu giá của một seller cụ thể (thường dùng khi ban
    // user)
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