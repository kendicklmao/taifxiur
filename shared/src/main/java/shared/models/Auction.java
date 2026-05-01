package shared.models;

import shared.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

// Phiên đấu giá
public class Auction {

    private static final long EXTEND_THRESHOLD = 10; // Số giây còn lại để kéo dài phiên đấu giá
    private static final long EXTEND_TIME = 20; // Số giây cộng thêm khi kéo dài phiên đấu giá
    private static final ScheduledExecutorService globalScheduler = Executors.newScheduledThreadPool(4); // Lịch

    private final String id; // Mã phiên đấu giá
    private final Item item; // Mặt hàng đấu giá
    private final BigDecimal startPrice; // Giá khởi điểm
    private final Seller seller; // Người bán
    private final Instant startTime; // Thời gian bắt đầu
    private final List<BidTransaction> bidHistory = new ArrayList<>(); // Lịch sử đặt giá
    private final Object bidLock = new Object(); // Lock cho việc đặt giá và đấu giá tự động
    private final PriorityQueue<AutoBid> autoBids = new PriorityQueue<>((a, b) -> {
        int cmp = b.getMaxBid().compareTo(a.getMaxBid()); // maxBid cao hơn sẽ thắng
        if (cmp != 0) {
            return cmp;
        }
        return a.getTimeStamp().compareTo(b.getTimeStamp()); // Đăng ký sớm hơn sẽ thắng
    });
    
    private BigDecimal currentPrice; // Giá hiện tại
    private Bidder highestBidder; // Người thắng phiên
    private Instant endTime; // Thời gian kết thúc
    private AuctionStatus status; // Trạng thái phiên đấu giá
    private ScheduledFuture<?> finishTask; // Kết thúc phiên đấu giá
    private transient Consumer<Auction> finishCallback; // Callback khi kết thúc phiên đấu giá
    private transient Predicate<String> banChecker; // Kiểm tra người bị cấm

    public Auction(String id, Item item, BigDecimal startPrice, Seller seller, Instant startTime, Instant endTime) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Auction ID is null or blank");
        }
        if (item == null) {
            throw new IllegalArgumentException("Auction item is null");
        }
        if (startPrice == null || startPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid start price: " + startPrice);
        }
        if (seller == null) {
            throw new IllegalArgumentException("Auction seller is null");
        }
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Invalid auction dates: start = " + startTime + ", end = " + endTime);
        }

        this.id = id;
        this.item = item;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.seller = seller;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = startTime.isBefore(Instant.now()) ? AuctionStatus.RUNNING : AuctionStatus.OPEN;
        scheduleStart();
        scheduleFinish();
    }

    public void cancel() {
        synchronized (bidLock) {
            if (finishTask != null) {
                finishTask.cancel(true);
            }
            status = AuctionStatus.CANCELED;
        }
    }

    public void setFinishCallback(Consumer<Auction> cb) {
        this.finishCallback = cb;
    }

    public void setBanChecker(Predicate<String> checker) {
        this.banChecker = checker;
    }

    // Hệ thống đấu giá tự động
    public void AutoBidService() {
        synchronized (bidLock) {
            
            if (autoBids.isEmpty() || status != AuctionStatus.RUNNING) {
                return;
            }

            AutoBid highest = autoBids.poll();
            
            // Kiểm tra xem người có bid cao nhất có bị cấm không
            if (banChecker != null && banChecker.test(highest.getBidder().getUsername())) {
                System.out.println("DEBUG AUTOBID: User " + highest.getBidder().getUsername() + " is banned. Removing autobid.");
                return; // Bỏ qua chu kỳ autobid này
            }

            AutoBid second = autoBids.peek();

            BigDecimal increment = item.getMinIncrement();
            BigDecimal newPrice;

            if (highest.getBidder().equals(highestBidder)) {
                // Nếu người có bid cao nhất đã thắng thì outbid bid cao thứ 2 nếu có
                if (second != null) {
                    newPrice = second.getMaxBid().add(increment);
                    if (newPrice.compareTo(highest.getMaxBid()) > 0) {
                        newPrice = highest.getMaxBid();
                    }
                    if (newPrice.compareTo(currentPrice) > 0) {
                        currentPrice = newPrice;
                        bidHistory.add(new BidTransaction(highest.getBidder(), newPrice, Instant.now()));
                    }
                }
            } else {
                // Người khác đang thắng. Người có bid cao nhất cần outbid họ
                BigDecimal minToOutbid = currentPrice.add(increment);
                if (second != null) {
                    newPrice = second.getMaxBid().add(increment);
                    if (newPrice.compareTo(minToOutbid) < 0) {
                        newPrice = minToOutbid;
                    }
                } else {
                    newPrice = minToOutbid;
                }

                if (newPrice.compareTo(highest.getMaxBid()) > 0) {
                    newPrice = highest.getMaxBid();
                }

                if (newPrice.compareTo(currentPrice.add(increment)) >= 0) {
                    currentPrice = newPrice;
                    highestBidder = highest.getBidder();
                    bidHistory.add(new BidTransaction(highest.getBidder(), newPrice, Instant.now()));
                }
            }
            autoBids.add(highest);
        }
    }

    // Đăng kí đấu giá tự động
    public void registerAutoBid(Bidder bidder, BigDecimal maxBid) {
        synchronized (bidLock) {
            if (bidder == null || maxBid == null) {
                throw new IllegalArgumentException();
            }
            if (maxBid.compareTo(currentPrice.add(item.getMinIncrement())) < 0) {
                throw new IllegalArgumentException("Bid amount must be at least current price + minimum increment");
            }
            if (bidder.isBanned()) {
                throw new IllegalArgumentException();
            }
            if (status != AuctionStatus.RUNNING && status != AuctionStatus.OPEN) {
                throw new IllegalArgumentException("Auction is not in a biddable state");
            }
            autoBids.add(new AutoBid(bidder, maxBid));
            AutoBidService();
        }
    }

    // Bắt đầu phiên giao dịch
    private void scheduleStart() {
        long delay = startTime.getEpochSecond() - Instant.now().getEpochSecond();
        if (delay < 0) {
            delay = 0;
        }
        globalScheduler.schedule(() -> {
            synchronized (bidLock) {
                if (status == AuctionStatus.OPEN) {
                    status = AuctionStatus.RUNNING;
                }
            }
        }, delay, TimeUnit.SECONDS);
    }

    // Kết thúc phiên giao dịch
    private void scheduleFinish() {
        long delay = endTime.getEpochSecond() - Instant.now().getEpochSecond();
        if (delay < 0) {
            delay = 0;
        }
        finishTask = globalScheduler.schedule(() -> {
            synchronized (bidLock) {
                if (status == AuctionStatus.RUNNING && !Instant.now().isBefore(endTime)) {
                    status = AuctionStatus.FINISHED;
                    // Thông báo cho người thắng phiên đấu giá
                    try {
                        if (finishCallback != null) {
                            finishCallback.accept(this);
                        }
                    } catch (Exception ignored) {

                    }
                }
            }
        }, delay, TimeUnit.SECONDS);
    }

    // Bidder thắng trả tiền
    public void itemPaid(Bidder bidder) {
        synchronized (bidLock) {
            if (status != AuctionStatus.FINISHED) {
                throw new IllegalStateException();
            }
            if (highestBidder == null || !highestBidder.equals(bidder)) {
                throw new IllegalStateException();
            }
            BigDecimal price = currentPrice;
            Seller seller = this.seller;
            boolean success = bidder.getWallet().transfer(price, seller);
            if (!success) {
                throw new IllegalStateException();
            }
            status = AuctionStatus.PAID;
        }
    }

    // Đặt giá
    public boolean placeBid(Bidder bidder, BigDecimal amount) {
        synchronized (bidLock) {
            if (bidder == null || amount == null) {
                throw new IllegalArgumentException();
            }
            if (bidder.isBanned()) {
                throw new IllegalStateException("User is banned");
            }
            if (status != AuctionStatus.RUNNING) {
                throw new IllegalStateException("Auction is not running. Current status: " + status);
            }
            if (Instant.now().isAfter(endTime)) {
                status = AuctionStatus.FINISHED;
                return false;
            }
            if (amount.compareTo(currentPrice.add(item.getMinIncrement())) < 0) {
                return false;
            }
            currentPrice = amount;
            highestBidder = bidder;
            bidHistory.add(new BidTransaction(bidder, amount, Instant.now()));
            AutoBidService();
            extendIfNeeded();
            return true;
        }
    }

    // Đặt lại giá trị bid từ DB
    public void restoreBid(Bidder bidder, BigDecimal amount, Instant timestamp) {
        synchronized (bidLock) {
            if (currentPrice == null || amount.compareTo(currentPrice) > 0) {
                currentPrice = amount;
                highestBidder = bidder;
            }
            bidHistory.add(new BidTransaction(bidder, amount, timestamp));
        }
    }

    // Kéo dài phiên đấu giá nếu cần
    private void extendIfNeeded() {
        synchronized (bidLock) {
            long remaining = endTime.getEpochSecond() - Instant.now().getEpochSecond();
            if (remaining <= EXTEND_THRESHOLD) {
                endTime = endTime.plusSeconds(EXTEND_TIME);
                if (finishTask != null) {
                    finishTask.cancel(false);
                }
                scheduleFinish();
            }
        }
    }

    public String getId() {
        return id;
    }

    public Item getItem() {
        return item;

    }

    public BigDecimal getCurrentPrice() {
        synchronized (bidLock) {
            return currentPrice;
        }
    }

    public Bidder getHighestBidder() {
        synchronized (bidLock) {
            return highestBidder;
        }
    }

    public AuctionStatus getStatus() {
        synchronized (bidLock) {
            return status;
        }
    }

    public void setStatus(AuctionStatus status) {
        synchronized (bidLock) {
            this.status = status;
        }
    }

    public List<BidTransaction> getBidHistory() {
        synchronized (bidLock) {
            return new ArrayList<>(bidHistory);
        }
    }

    public Seller getSeller() {
        return seller;
    }

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Cập nhật trạng thái của phiên đấu giá dựa trên thời gian hiện tại.
     * Chỉ nên được gọi bởi các service đáng tin cậy.
     */
    public void updateStatus() {
        synchronized (bidLock) {
            Instant now = Instant.now();
            if (status == AuctionStatus.OPEN && !now.isBefore(startTime)) {
                status = AuctionStatus.RUNNING;
            } else if (status == AuctionStatus.RUNNING && !now.isBefore(endTime)) {
                status = AuctionStatus.FINISHED;
            }
        }
    }

    /**
     * Đặt giá hiện tại khi khôi phục từ cơ sở dữ liệu.
     * Chỉ nên được gọi bởi các service đáng tin cậy.
     */
    public void setCurrentPriceForDBRestore(BigDecimal price) {
        synchronized (bidLock) {
            this.currentPrice = price;
        }
    }

    /**
     * Đặt trạng thái khi khôi phục từ cơ sở dữ liệu.
     * Chỉ nên được gọi bởi các service đáng tin cậy.
     */
    public void setStatusForDBRestore(AuctionStatus status) {
        synchronized (bidLock) {
            this.status = status;
        }
    }
}