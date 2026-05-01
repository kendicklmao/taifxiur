package shared.models;

import shared.enums.ItemStatus;
import shared.enums.Role;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Người bán hàng
public class Seller extends User {
    private final transient ConcurrentHashMap<String, Item> items = new ConcurrentHashMap<>(); // Danh sách sản phẩm

    public Seller(int id, String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(id, username, password, email, Role.SELLER, q1, a1, q2, a2);
    }

    public Seller(String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(0, username, password, email, Role.SELLER, q1, a1, q2, a2);
    }

    public Seller(int id, String username, String hashedPassword, String passwordSalt, String email, 
                  boolean isBanned, String q1, String hashedA1, String saltA1, String q2, String hashedA2, String saltA2) {
        super(id, username, hashedPassword, passwordSalt, email, Role.SELLER, isBanned, q1, hashedA1, saltA1, q2, hashedA2, saltA2);
    }

    // Tạo sản phẩm nghệ thuật
    public void createArt(String name, String description, BigDecimal startingPrice, String artist, int yearCreated, boolean isOriginal) {
        Art a = new Art(name, description, this, startingPrice, artist, yearCreated, isOriginal);
        if (!a.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(a);
    }

    // Tạo sản phẩm điện tử
    public void createElectronic(String name, String description, BigDecimal startingPrice, String brand, ItemStatus status) {
        Electronic e = new Electronic(name, description, this, startingPrice, brand, status);
        if (!e.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(e);
    }

    // Tạo sản phẩm thời trang
    public void createFashion(String name, String description, BigDecimal startingPrice, String brand, ItemStatus status) {
        Fashion f = new Fashion(name, description, this, startingPrice, brand, status);
        if (!f.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(f);
    }

    // Tạo sản phẩm sưu tầm
    public void createCollectible(String name, String description, BigDecimal startingPrice, int yearCreated) {
        Collectible c = new Collectible(name, description, this, startingPrice, yearCreated);
        if (!c.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(c);
    }

    // Tạo sản phẩm phương tiện
    public void createVehicle(String name, String description, BigDecimal startingPrice, String brand, int model, int kmTravel) {
        Vehicle v = new Vehicle(name, description, this, startingPrice, brand, model, kmTravel);
        if (!v.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(v);
    }

    // Thêm sản phẩm
    public void addItem(Item item) {
        if (item == null || !item.isValid()) {
            throw new IllegalArgumentException();
        }
        String id = UUID.randomUUID().toString();
        items.put(id, item);
    }

    // Tìm sản phẩm theo id
    public Item getItem(String id) {
        return items.get(id);
    }

    // Xóa sản phẩm theo id
    public void removeItem(String id) {
        items.remove(id);
    }

    // Xem tất cả sản phẩm
    public Collection<Item> getAllItems() {
        return items.values();
    }
}