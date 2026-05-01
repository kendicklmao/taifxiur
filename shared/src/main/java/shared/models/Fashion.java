package shared.models;

import shared.enums.Category;
import shared.enums.ItemStatus;

import java.math.BigDecimal;

public class Fashion extends Item {
    private final String brand; //hãng
    private final ItemStatus status; //trạng thái mặt hàng: new || like new || used

    public Fashion(String name, String description, Seller seller, BigDecimal startingPrice, String brand, ItemStatus status) {
        super(name, description, seller, Category.FASHIONS, startingPrice, null, null);
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