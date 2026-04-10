package model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.*;

public class Auction {          //1 phiên giao dịch
    private final String id;    //mã phiên giao dịch
    private final Item item;    // mặt hàng giao dịch
    private final BigDecimal startPrice;//giá khởi điểm
    private BigDecimal currentPrice;//giá hiện tại
    private final Seller seller;//người bán
    private Bidder highestBidder;//người thắng phiên
    private Instant startTime;//thời gian bắt đầu
    private Instant endTime;//thời gian kết thúc
    private AuctionStatus status;//trạng thái phiên giao dịch
    private final List<BidTransaction> bidHistory = new ArrayList<>(); //lịch sử đặt giá
    private final Object bidLock = new Object(); //thread safe
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4); //thread safe
    private ScheduledFuture<?> finishTask; //kết thúc giao dịch
    private static final long EXTEND_THRESHOLD = 10; //số giây còn lại để kéo dài giao dịch
    private static final long EXTEND_TIME = 20; //số giây cộng thêm khi kéo dài giao dịch    
    private int extendCount = 1;
    private final PriorityQueue<AutoBid> autoBids = new PriorityQueue<>((a, b) -> {
    int cmp = b.getMaxBid().compareTo(a.getMaxBid()); // maxBid cao hơn sẽ thắng
    if (cmp != 0){
        return cmp;
    }
    return a.getTimeStamp().compareTo(b.getTimeStamp()); // đăng ký sớm hơn sẽ thắng
    });  

    public void AutoBidService(){
        synchronized(bidLock){
            if (autoBids.isEmpty()){
                return;
            }
            AutoBid highest = autoBids.poll();
            AutoBid second = autoBids.peek();
            BigDecimal newPrice;
            if (highest == null){
                throw new IllegalArgumentException();
            }
            else if (second == null){
                newPrice = currentPrice.add(item.getMinIncrement());
                if (newPrice.compareTo(highest.getMaxBid()) <= 0){
                    currentPrice = newPrice;
                    highestBidder = highest.getBidder();
                    bidHistory.add(new BidTransaction(highest.getBidder(), newPrice, Instant.now()));
                    autoBids.add(highest);
                }
            }
            else{
                newPrice = second.getMaxBid().add(item.getMinIncrement());
                BigDecimal minNext = currentPrice.add(item.getMinIncrement());
                if (newPrice.compareTo(minNext) < 0){
                    newPrice = minNext;
                }
                if (newPrice.compareTo(highest.getMaxBid()) <= 0){
                    currentPrice = newPrice;
                    highestBidder = highest.getBidder();
                    bidHistory.add(new BidTransaction(highest.getBidder(), newPrice, Instant.now()));
                    autoBids.add(highest);
                }
            }
        }
    }
    public void registerAutoBid(Bidder bidder, BigDecimal maxBid){
        synchronized(bidLock){
            if (bidder == null || maxBid == null){
                throw new IllegalArgumentException();
            }
            if (bidder.getWallet().getBalance().compareTo(maxBid) < 0){
                throw new IllegalArgumentException();
            }
            if (maxBid.compareTo(currentPrice.add(item.getMinIncrement())) <= 0){
                throw new IllegalArgumentException();
            }
            if (bidder.isBanned()){
                throw new IllegalArgumentException();
            }
            if (status != AuctionStatus.RUNNING){
                throw new IllegalArgumentException();
            }
            autoBids.add(new AutoBid(bidder, maxBid));
            AutoBidService();
        }
    }
    public Auction(String id, Item   item, BigDecimal startPrice, Seller seller, Instant startTime, Instant endTime) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException();
        if (item == null)
            throw new IllegalArgumentException();
        if (startPrice == null || startPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException();
        if (seller == null)
            throw new IllegalArgumentException();
        if (startTime == null || endTime == null || endTime.isBefore(startTime))
            throw new IllegalArgumentException();
        this.id = id;
        this.item = item;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.seller = seller;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        scheduleStart();
        scheduleFinish();
    }
    private void scheduleStart() {//bắt đầu phiên giao dịch
        long delay = startTime.getEpochSecond() - Instant.now().getEpochSecond();
        if (delay < 0) delay = 0;
        scheduler.schedule(() -> {
            synchronized (bidLock) {
                if (status == AuctionStatus.OPEN) {
                    status = AuctionStatus.RUNNING;
                }
            }
        }
        , delay, TimeUnit.SECONDS);
    }
    private void scheduleFinish() {// kết thúc phiên giao dịch
        long delay = endTime.getEpochSecond() - Instant.now().getEpochSecond();
        if (delay < 0) delay = 0;
        finishTask = scheduler.schedule(() -> {
            synchronized (bidLock) {
                if (status == AuctionStatus.RUNNING && !Instant.now().isBefore(endTime)) {
                    status = AuctionStatus.FINISHED;
                    scheduler.shutdown();
                }
            }
        }
        , delay, TimeUnit.SECONDS);
    }
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
    public boolean placeBid(Bidder bidder, BigDecimal amount) {//đặt giá
        synchronized (bidLock) {
            if (bidder == null || amount == null)
                throw new IllegalArgumentException();
            if (bidder.isBanned())
                throw new IllegalStateException();
            if (bidder.getWallet().getBalance().compareTo(amount) < 0){
                throw new IllegalArgumentException();
            }
            if (status != AuctionStatus.RUNNING)
                throw new IllegalStateException();
            if (Instant.now().isAfter(endTime)) {
                status = AuctionStatus.FINISHED;
                return false;
            }
            if (amount.compareTo(currentPrice.add(item.getMinIncrement())) <= 0)
                return false;
            currentPrice = amount;
            highestBidder = bidder;
            bidHistory.add(new BidTransaction(bidder, amount, Instant.now()));
            AutoBidService();
            extendIfNeeded();
            return true;
        }
    }
    private void extendIfNeeded() {//kéo dài phiên giao dịch
        synchronized (bidLock) {
            long remaining = endTime.getEpochSecond() - Instant.now().getEpochSecond();
            if (remaining <= EXTEND_THRESHOLD && extendCount <= 3) {
                endTime = endTime.plusSeconds(EXTEND_TIME);
                extendCount ++;
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
}