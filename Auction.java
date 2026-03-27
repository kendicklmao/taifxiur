import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Auction {

    private String id;
    private String itemName;
    private double startPrice;
    private double currentPrice;
    private Seller seller;
    private Bidder highestBidder;
    private Instant startTime;
    private Instant endTime;
    private AuctionStatus status;
    private static final ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(1);

    private final List<BidTransaction> bidHistory = new ArrayList<>();

    public Auction(String id, String itemName, double startPrice,
                   Seller seller, Instant startTime, Instant endTime) {

        if (itemName == null || itemName.trim().isEmpty()) return;
        if (startPrice < 0) return;
        if (seller == null) return;
        if (startTime == null || endTime == null) return;
        if (endTime.isBefore(startTime)) return;

        this.id = id;
        this.itemName = itemName.trim();
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.seller = seller;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;

        scheduleStart();
        scheduleFinish();
    }
    private void scheduleStart() {
    long delay = startTime.getEpochSecond() - Instant.now().getEpochSecond();
    if (delay < 0) delay = 0;

    scheduler.schedule(() -> {
        synchronized (this) {
            if (status == AuctionStatus.OPEN) {
                status = AuctionStatus.RUNNING;
            }
        }
    }, delay, TimeUnit.SECONDS);
}

private void scheduleFinish() {
    long delay = endTime.getEpochSecond() - Instant.now().getEpochSecond();
    if (delay < 0) delay = 0;

    scheduler.schedule(() -> {
        synchronized (this) {
            if (status == AuctionStatus.RUNNING || status == AuctionStatus.OPEN) {
                status = AuctionStatus.FINISHED;
            }
        }
    }, delay, TimeUnit.SECONDS);
}

    public synchronized boolean placeBid(Bidder bidder, double amount) {
    if (bidder == null || bidder.isBanned()) return false;

    if (status != AuctionStatus.RUNNING) return false;
    if (Instant.now().isAfter(endTime)) {
        status = AuctionStatus.FINISHED;
        return false;
    }

    if (amount <= currentPrice) return false;

    currentPrice = amount;
    highestBidder = bidder;
    bidHistory.add(new BidTransaction(bidder, amount, Instant.now()));

    return true;
}

    public synchronized void start() {
        if (status == AuctionStatus.OPEN && Instant.now().isAfter(startTime)) {
            status = AuctionStatus.RUNNING;
        }
    }

    public synchronized void finish() {
        if (status == AuctionStatus.RUNNING && Instant.now().isAfter(endTime)) {
            status = AuctionStatus.FINISHED;
        }
    }

    public String getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public Bidder getHighestBidder() {
        return highestBidder;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public List<BidTransaction> getBidHistory() {
        return new ArrayList<>(bidHistory);
    }
}