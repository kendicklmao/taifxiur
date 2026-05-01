package shared.models;

import shared.enums.Category;
import shared.enums.ItemStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Electronic extends Item { // Mặt hàng điện tử
    private final String brand; // Hãng
    private final ItemStatus status; // Trạng thái mặt hàng (New, Like New, Used)

    public Electronic(String name, String description, Seller seller, BigDecimal startingPrice, String brand, ItemStatus status, LocalDateTime startTime, LocalDateTime endTime) {
        super(name, description, seller, Category.ELECTRONICS, startingPrice, startTime, endTime);
        this.brand = brand;
        this.status = status;
    }

    public String getBrand() {
        return brand;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public boolean isValid() {
        return super.isValid();
    }
}