package shared.models;

import shared.enums.Category;
import shared.enums.ItemStatus;

import java.math.BigDecimal;

public class Electronic extends Item { //mặt hàng điện tử
    private final String brand; //hãng
    private final ItemStatus status;//trạng thái mặt hàng: new || like new || used

    public Electronic(String name, String description, Seller seller, String brand, ItemStatus status) {
        super(null, name, description, null, null, null, seller, Category.ELECTRONICS);
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