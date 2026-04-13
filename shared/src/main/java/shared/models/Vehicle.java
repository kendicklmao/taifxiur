package shared.models;

import shared.enums.Category;

import java.math.BigDecimal;

public class Vehicle extends Item {//mặt hàng xe cộ
    private final String brand;//hãng
    private final int model;//năm sản xuất
    private final int kmTravel; //số km đã đi

    public Vehicle(String name, String description, Seller seller, String brand, int model, int kmTravel) {
        super(name, description, seller, Category.VEHICLES);
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
        if (model < 0) {
            return false;
        }
        return kmTravel >= 0;
    }
}