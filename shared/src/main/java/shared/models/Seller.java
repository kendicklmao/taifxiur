package shared.models;

import shared.enums.ItemStatus;
import shared.enums.Role;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Seller extends User {//người bán hàng
    private final ConcurrentHashMap<String, Item> items = new ConcurrentHashMap<>(); //danh sách sản phẩm
    private final Wallet wallet = new Wallet();

    public Seller(String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(username, password, email, Role.SELLER, q1, a1, q2, a2);
    }

    public void createArt(String name, String description, String artist, int yearCreated, boolean isOriginal) {//tạo sản phẩm nghệ thuật
        Art a = new Art(name, description, this, artist, yearCreated, isOriginal);
        if (!a.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(a);
    }

    public void createElectronic(String name, String description, String brand, ItemStatus status) { //tạo sản phẩm điện tử
        Electronic e = new Electronic(name, description, this, brand, status);
        if (!e.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(e);
    }

    public void createFashion(String name, String description, String brand, ItemStatus status) {//tạo sản phẩm thời trang
        Fashion f = new Fashion(name, description, this, brand, status);
        if (!f.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(f);
    }

    public void createCollectible(String name, String description, int yearCreated) {//tảo sản phẩm sưu tầm
        Collectible c = new Collectible(name, description, this, yearCreated);
        if (!c.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(c);
    }

    public void createVehicle(String name, String description, String brand, int model, int kmTravel) {//tạo sản phẩm phương tiện
        Vehicle v = new Vehicle(name, description, this, brand, model, kmTravel);
        if (!v.isValid()) {
            throw new IllegalArgumentException();
        }
        addItem(v);
    }

    public void addItem(Item item) {//thêm sản phẩm
        if (item == null || !item.isValid()) {
            throw new IllegalArgumentException();
        }
        String id = UUID.randomUUID().toString();
        items.put(id, item);
    }

    public Item getItem(String id) {//tìm sản phẩm theo id
        return items.get(id);
    }

    public void removeItem(String id) {//xóa sản phẩm theo id
        items.remove(id);
    }

    public Collection<Item> getAllItems() {//xem tất cả sản phẩm
        return items.values();
    }

    public Wallet getWallet() {
        return wallet;
    }

}