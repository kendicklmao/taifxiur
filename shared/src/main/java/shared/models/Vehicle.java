package shared.models;

import shared.enums.Category;

import java.math.BigDecimal;

// Mặt hàng xe cộ
public class Vehicle extends Item {
    private final String brand; // Hãng
    private final int model; // Năm sản xuất
    private final int kmTravel; // Số km đã đi

    public Vehicle(String name, String description, Seller seller, BigDecimal startingPrice, String brand, int model, int kmTravel) {
        super(name, description, seller, Category.VEHICLES, startingPrice);
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

    // Kiểm tra thông số có logic không?
    public boolean isValid() {
        return super.isValid() && model >= 0 && kmTravel >= 0;
    }
}