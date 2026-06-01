package shared.models.items;

import shared.enums.Category;
import shared.models.users.Seller;

import java.math.BigDecimal;
import java.util.Map;
import java.util.LinkedHashMap;

// Mặt hàng sưu tập
public class Collectible extends Item {
    private int yearCreated;

    public Collectible(String name, String description, Seller seller, BigDecimal startingPrice, int yearCreated) {
        super(name, description, seller, Category.COLLECTIBLES, startingPrice);
        this.yearCreated = yearCreated;
    }
    
    @Override
    public Map<String, String> getAdditionalDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Year Created", String.valueOf(yearCreated));
        return details;
    }

    public int getYearCreated() {
        return yearCreated;
    }

    public boolean isValid() {
        return super.isValid() && yearCreated >= 0;
    }
}