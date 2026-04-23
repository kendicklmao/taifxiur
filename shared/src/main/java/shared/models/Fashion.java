package shared.models;

import shared.enums.Category;
import shared.enums.ItemStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Fashion extends Item {
    private final String brand; //hãng
    private final ItemStatus status; //trạng thái mặt hàng: new || like new || used

    public Fashion(String name, String description, Seller seller, String brand, ItemStatus status, LocalDateTime startTime, LocalDateTime endTime) {
        super(null, name, description, null, null, null, seller, Category.FASHIONS, startTime, endTime);
        this.brand = brand;
        this.status = status;
    }

    public String getBrand() {
        return brand;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public BigDecimal getMinIncrement() { //số tiền lớn hơn tối thiểu so với giá hiện tại là 100k
        return new BigDecimal("100000");
    }

    public boolean isValid() {
        return super.isValid();
    }
}