package shared.models;

import shared.enums.Category;

public class Vehicle extends Item {//mặt hàng xe cộ
    private final String brand;//hãng
    private final int model;//năm sản xuất
    private final int kmTravel; //số km đã đi

    public Vehicle(String name, String description, Seller seller, String brand, int model, int kmTravel) {
        super(name, description, seller, Category.VEHICLES, null, null);
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



    public boolean isValid() {//kiểm tra thông số có logic không ?
        return super.isValid() && model >= 0 && kmTravel >= 0;
    }
}