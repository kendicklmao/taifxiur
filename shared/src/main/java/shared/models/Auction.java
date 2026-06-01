package shared.models;

import shared.enums.AuctionStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
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
    private transient Consumer<Auction> statusChangeListener; // Callback khi trạng thái thay đổi
    private transient Predicate<String> banChecker; // Kiểm tra người bị cấm
    private transient BiConsumer<Bidder, BigDecimal> bidListener; // Callback khi có bid mới
    private transient Consumer<Auction> endTimeChangeListener; // Callback khi thời gian kết thúc thay đổi

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
    }

    public void startScheduler() {
        scheduleStart();
        scheduleFinish();
    }

    public void setBidListener(BiConsumer<Bidder, BigDecimal> listener) {
        this.bidListener = listener;
    }

    public void setFinishCallback(Consumer<Auction> cb) {
        this.finishCallback = cb;
    }

    public void setStatusChangeListener(Consumer<Auction> listener) {
        this.statusChangeListener = listener;
    }

    public void setBanChecker(Predicate<String> checker) {
        this.banChecker = checker;
    }

    // Hệ thống đấu giá tự động
    public void AutoBidService() {
        synchronized (bidLock) {
            if (status != AuctionStatus.RUNNING) {
                return;
            }

            // Lọc ra các autobid hợp lệ
            List<AutoBid> activeList = new ArrayList<>();
            for (AutoBid ab: autoBids) {
                if (banChecker == null || !banChecker.test(ab.getBidder().getUsername())) {
                    activeList.add(ab);
                }
            }

            if (activeList.isEmpty()) {
                return;
            }

            // Sắp xếp giảm dần theo maxBid, nếu bằng nhau thì thời gian đăng ký sớm hơn lên trước
            activeList.sort((a, b) -> {
                int cmp = b.getMaxBid().compareTo(a.getMaxBid());
                if (cmp != 0) {
                    return cmp;
                }
                return a.getTimeStamp().compareTo(b.getTimeStamp());
            });

            BigDecimal increment = item.getMinIncrement();
            Bidder newHighestBidder = highestBidder;
            BigDecimal newCurrentPrice = currentPrice;

            // Vòng lặp mô phỏng cuộc đấu giá giữa các auto-bidder và giá hiện tại
            boolean bidPlaced;
            do {
                bidPlaced = false;

                // Tìm challenger: người có maxBid cao nhất trong số những người CHƯA phải là newHighestBidder
                AutoBid challenger = null;
                for (AutoBid ab : activeList) {
                    if (newHighestBidder == null || !newHighestBidder.getUsername().equals(ab.getBidder().getUsername())) {
                        if (challenger == null || ab.getMaxBid().compareTo(challenger.getMaxBid()) > 0) {
                            challenger = ab;
                        } else if (ab.getMaxBid().compareTo(challenger.getMaxBid()) == 0) {
                            if (ab.getTimeStamp().isBefore(challenger.getTimeStamp())) {
                                challenger = ab;
                            }
                        }
                    }
                }

                if (challenger != null) {
                    BigDecimal nextPrice = (newHighestBidder == null) ? startPrice : newCurrentPrice.add(increment);

                    if (challenger.getMaxBid().compareTo(nextPrice) >= 0) {
                        newHighestBidder = challenger.getBidder();
                        newCurrentPrice = nextPrice;
                        bidPlaced = true;
                    } else if (challenger.getMaxBid().compareTo(newCurrentPrice) > 0) {
                        newCurrentPrice = challenger.getMaxBid();
                        bidPlaced = true;
                    }
                }
            } while (bidPlaced);

            // Post-processing: nếu có ít nhất 2 auto-bidder thì người có maxBid cao nhất (ab1)
            // sẽ thắng. Giá cuối cùng là min(ab1.maxBid, ab2.maxBid + increment).
            if (activeList.size() >= 2) {
                AutoBid ab1 = activeList.get(0); // top max
                AutoBid ab2 = activeList.get(1); // second max

                BigDecimal topMax = ab1.getMaxBid();
                BigDecimal secondMax = ab2.getMaxBid();

                newHighestBidder = ab1.getBidder();
                BigDecimal clearingPrice = secondMax.add(increment);
                // If topMax < secondMax + increment then winner pays topMax, otherwise pays secondMax + increment
                if (topMax.compareTo(clearingPrice) < 0) {
                    newCurrentPrice = topMax;
                } else {
                    newCurrentPrice = clearingPrice;
                }
            }

            newCurrentPrice = newCurrentPrice.setScale(2, RoundingMode.UP);

            // Chỉ cập nhật và ghi nhận giao dịch nếu giá trị mới khác với giá hiện tại hoặc người thắng thay đổi
            boolean changed = (newCurrentPrice.compareTo(currentPrice) != 0) ||
                              (highestBidder == null) ||
                              (!highestBidder.getUsername().equals(newHighestBidder.getUsername()));

            if (changed) {
                currentPrice = newCurrentPrice;
                highestBidder = newHighestBidder;
                bidHistory.add(new BidTransaction(newHighestBidder, newCurrentPrice, Instant.now()));
                if (bidListener != null) {
                    bidListener.accept(newHighestBidder, newCurrentPrice);
                }
            }
        }
    }

    // Đăng kí đấu giá tự động
    public void registerAutoBid(Bidder bidder, BigDecimal maxBid) {
        registerAutoBid(bidder, maxBid, Instant.now());
    }

    public void registerAutoBid(Bidder bidder, BigDecimal maxBid, Instant timeStamp) {
        synchronized (bidLock) {
            if (bidder == null || maxBid == null) {
                throw new IllegalArgumentException();
            }

            maxBid = maxBid.setScale(2, RoundingMode.UP);

            BigDecimal minRequired = (highestBidder != null && highestBidder.equals(bidder))
                    ? currentPrice
                    : (highestBidder == null ? startPrice : currentPrice.add(item.getMinIncrement()));

            if (maxBid.compareTo(minRequired) < 0) {
                throw new IllegalArgumentException("Bid amount must be at least " + minRequired);
            }

            if (bidder.isBanned()) {
                throw new IllegalArgumentException();
            }

            if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
                throw new IllegalArgumentException("Auction is not in a biddable state (must be OPEN or RUNNING)");
            }

            // Xóa auto-bid cũ của người này nếu có để tránh trùng lặp
            autoBids.removeIf(ab -> ab.getBidder().getUsername().equals(bidder.getUsername()));

            autoBids.add(new AutoBid(bidder, maxBid, timeStamp));
            AutoBidService();
            extendIfNeeded();
        }
    }

    public void restoreAutoBid(Bidder bidder, BigDecimal maxBid, Instant timeStamp) {
        synchronized (bidLock) {
            if (bidder == null || maxBid == null) {
                throw new IllegalArgumentException();
            }
            maxBid = maxBid.setScale(2, RoundingMode.UP);
            autoBids.removeIf(ab -> ab.getBidder().getUsername().equals(bidder.getUsername()));
            autoBids.add(new AutoBid(bidder, maxBid, timeStamp));
        }
    }

    // Bắt đầu phiên giao dịch
    private void scheduleStart() {
        long delay = startTime.getEpochSecond() - Instant.now().getEpochSecond();
        if (delay <= 0) {
            synchronized (bidLock) {
                if (status == AuctionStatus.OPEN) {
                    status = AuctionStatus.RUNNING;
                    if (statusChangeListener != null) {
                        statusChangeListener.accept(this);
                    }
                }
            }
            return;
        }

        globalScheduler.schedule(() -> {
            synchronized (bidLock) {
                if (status == AuctionStatus.OPEN) {
                    status = AuctionStatus.RUNNING;
                    if (statusChangeListener != null) {
                        statusChangeListener.accept(this);
                    }
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
    public void payItem(Bidder bidder) {
        synchronized (bidLock) {
            if (status != AuctionStatus.FINISHED) {
                throw new IllegalStateException("Auction is not in FINISHED status");
            }

            if (highestBidder == null || !highestBidder.getUsername().equals(bidder.getUsername())) {
                throw new IllegalStateException("User is not the winner of this auction");
            }

            BigDecimal price = currentPrice;
            Seller seller = this.seller;
            boolean success = bidder.getWallet().transfer(price, seller);
            if (!success) {
                throw new IllegalStateException("In-memory wallet transfer failed");
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

            amount = amount.setScale(2, RoundingMode.UP);

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

            // Lần đầu tiên bid: chỉ cần >= startPrice
            // Đã có người bid: phải >= currentPrice + minIncrement
            BigDecimal minRequired = (highestBidder == null)
                    ? startPrice
                    : currentPrice.add(item.getMinIncrement());
            if (amount.compareTo(minRequired) < 0) {
                return false;
            }

            currentPrice = amount;
            highestBidder = bidder;
            bidHistory.add(new BidTransaction(bidder, amount, Instant.now()));
            if (bidListener != null) {
                bidListener.accept(bidder, amount);
            }
            AutoBidService();
            extendIfNeeded();
            return true;
        }
    }

    // Đặt lại giá trị bid từ DB
    public void restoreBid(Bidder bidder, BigDecimal amount, Instant timestamp) {
        synchronized (bidLock) {
            if (currentPrice == null || amount.compareTo(currentPrice) >= 0) {
                currentPrice = amount;
                highestBidder = bidder;
            }
            bidHistory.add(new BidTransaction(bidder, amount, timestamp));
        }
    }

    private void extendIfNeeded() {
        synchronized (bidLock) {
            if (status != AuctionStatus.RUNNING) {
                return;
            }
            long remaining = endTime.getEpochSecond() - Instant.now().getEpochSecond();
            if (remaining <= EXTEND_THRESHOLD) {
                endTime = endTime.plusSeconds(EXTEND_TIME);
                if (finishTask != null) {
                    finishTask.cancel(false);
                }
                scheduleFinish();
                if (endTimeChangeListener != null) {
                    endTimeChangeListener.accept(this);
                }
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

    public void setEndTime(Instant endTime) {
        synchronized (bidLock) {
            this.endTime = endTime;
        }
    }

    public void setEndTimeForDBRestore(Instant newEndTime) {
        synchronized (bidLock) {
            if (newEndTime == null) {
                return;
            }
            if (this.endTime != null && this.endTime.getEpochSecond() == newEndTime.getEpochSecond()) {
                return;
            }
            this.endTime = newEndTime;
            if (status == AuctionStatus.RUNNING) {
                if (finishTask != null) {
                    finishTask.cancel(false);
                }
                scheduleFinish();
            }
        }
    }

    public void setEndTimeChangeListener(Consumer<Auction> listener) {
        this.endTimeChangeListener = listener;
    }

    public void updateStatus() {
        synchronized (bidLock) {
            Instant now = Instant.now();
            if (status == AuctionStatus.OPEN && !now.isBefore(startTime)) {
                status = AuctionStatus.RUNNING;
                if (statusChangeListener != null) {
                    statusChangeListener.accept(this);
                }
            } else if (status == AuctionStatus.RUNNING && !now.isBefore(endTime)) {
                status = AuctionStatus.FINISHED;
                if (statusChangeListener != null) {
                    statusChangeListener.accept(this);
                }
            }
        }
    }

    public boolean hasBidder(String username) {
        synchronized (bidLock) {
            for (BidTransaction tx : bidHistory) {
                if (tx.getBidder().getUsername().equals(username)) {
                    return true;
                }
            }
            for (AutoBid ab : autoBids) {
                if (ab.getBidder().getUsername().equals(username)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void setCurrentPriceForDBRestore(BigDecimal price) {
        synchronized (bidLock) {
            this.currentPrice = price;
        }
    }

    public void setStatusForDBRestore(AuctionStatus status) {
        synchronized (bidLock) {
            this.status = status;
        }
    }

    public void clearBidHistory() {
        synchronized (bidLock) {
            this.bidHistory.clear();
            this.highestBidder = null;
            this.currentPrice = this.startPrice;
        }
    }

    public void clearAutoBids() {
        synchronized (bidLock) {
            this.autoBids.clear();
        }
    }

    public int getAutoBidsCount() {
        synchronized (bidLock) {
            return this.autoBids.size();
        }
    }

    public BigDecimal getAutoBidsSum() {
        synchronized (bidLock) {
            BigDecimal sum = BigDecimal.ZERO;
            for (AutoBid ab : this.autoBids) {
                sum = sum.add(ab.getMaxBid());
            }
            return sum;
        }
    }
}