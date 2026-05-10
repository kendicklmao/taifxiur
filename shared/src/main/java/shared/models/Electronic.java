package shared.models;

import shared.enums.Category;
import shared.enums.ItemStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.LinkedHashMap;

// Mặt hàng điện tử
public class Electronic extends Item {
    private final String brand; // Hãng
    private final ItemStatus status; // Trạng thái mặt hàng (New, Like New, Used)

    public Electronic(String name, String description, Seller seller, BigDecimal startingPrice, String brand, ItemStatus status) {
        super(name, description, seller, Category.ELECTRONICS, startingPrice);
        this.brand = brand;
        this.status = status;
    }

    @Override
    public Map<String, String> getAdditionalDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Brand", brand);
        details.put("Item Status", String.valueOf(status));
        return details;
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