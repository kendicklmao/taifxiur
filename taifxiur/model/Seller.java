package model;

import controller.AuctionService;
import controller.WalletService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Seller extends User {//người bán hàng
    private final ConcurrentHashMap<String, Item> items = new ConcurrentHashMap<>();
    private Wallet wallet = new Wallet();

    public Seller(String username, String password, String email, String q1, String a1, String q2, String a2){
        super(username, password, email, Role.SELLER, q1, a1, q2, a2);
    }
    public Auction createAuction(AuctionService service, Item item, BigDecimal startPrice, Instant startTime, Instant endTime) { //tạo 1 phiên giao dịch đấu giá
        if (service == null || item == null || startPrice == null || startTime == null || endTime == null){
            throw new IllegalArgumentException();
        }
        return service.createAuction(this, item, startPrice, startTime, endTime);
    }
    public void createArt(String name, String description, String artist, int yearCreated, boolean isOriginal) {
        Art a = new Art(name, description, this, artist, yearCreated, isOriginal);
        if (!a.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(a);
    }
    public void createElectronic(String name, String description, String brand, ItemStatus status) {
        Electronic e = new Electronic(name, description, this, brand, status);
        if (!e.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(e);;
    }
    public void createFashion(String name, String description, String brand, ItemStatus status) {
        Fashion f = new Fashion(name, description, this, brand, status);
        if (!f.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(f);;
    }
    public void createCollectible(String name, String description, int yearCreated) {
        Collectible c = new Collectible(name, description, this, yearCreated);
        if (!c.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(c);
    }
    public void createVehicle(String name, String description, String brand, int model, int kmTravel) {
        Vehicle v = new Vehicle(name, description, this, brand, model, kmTravel);
        if (!v.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(v);
    }
    public void addItem(Item item) {
        if (item == null || !item.isValid()) {
            throw new IllegalArgumentException();
        }
        String id = UUID.randomUUID().toString();
        items.put(id, item);
    }
    public Item getItem(String id) {
        return items.get(id);
    }
    public void removeItem(String id) {
        items.remove(id);
    }
    public Collection<Item> getAllItems() {
        return items.values();
    }
    public Wallet getWallet(){
        return wallet;
    }
    public void createWithdrawRequest(WalletService service, BigDecimal amount, BankInfo bankInfo){
        if (service == null || amount == null || bankInfo == null) {
            throw new IllegalArgumentException();
        }
        service.createWithdrawRequest(this, amount, bankInfo);
    }
}