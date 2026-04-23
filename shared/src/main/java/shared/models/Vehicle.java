package shared.models;

import shared.enums.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Vehicle extends Item {//mặt hàng xe cộ
    private final String brand;//hãng
    private final int model;//năm sản xuất
    private final int kmTravel; //số km đã đi

    public Vehicle(String name, String description, Seller seller, String brand, int model, int kmTravel, LocalDateTime startTime, LocalDateTime endTime) {
        super(null, name, description, null, null, null, seller, Category.VEHICLES, startTime, endTime);
        this.brand = brand;
        this.model = model;
        this.kmTravel = kmTravel;
    }

    public String getBrand() {
        return brand;
    }

    public int getModel() {
        return model;
    }

    public int getKMTravel() {
        return kmTravel;
    }

    public BigDecimal getMinIncrement() { //số tiền lớn hơn tối thiểu so với giá hiện tại là 10tr
        return new BigDecimal("10000000");
    }

    public boolean isValid() {//kiểm tra thông số có logic không ?
        return super.isValid() && model >= 0 && kmTravel >= 0;
    }
}