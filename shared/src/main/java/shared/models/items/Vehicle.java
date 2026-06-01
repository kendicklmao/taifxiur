package shared.models.items;

import shared.enums.Category;
import shared.models.users.Seller;

import java.math.BigDecimal;
import java.util.Map;
import java.util.LinkedHashMap;

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

    @Override
    public Map<String, String> getAdditionalDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Brand", brand);
        details.put("Model Year", String.valueOf(model));
        details.put("Km Traveled", String.valueOf(kmTravel));
        return details;
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

    public boolean isValid() {
        return super.isValid() && model >= 0 && kmTravel >= 0;
    }
}