package shared.models;

import shared.enums.Category;

import java.math.BigDecimal;

// Mặt hàng sưu tập
public class Collectible extends Item {
    private int yearCreated;

    public Collectible(String name, String description, Seller seller, BigDecimal startingPrice, int yearCreated) {
        super(name, description, seller, Category.COLLECTIBLES, startingPrice);
        this.yearCreated = yearCreated;
    }

    public int getYearCreated() {
        return yearCreated;
    }

    public boolean isValid() {
        return super.isValid() && yearCreated >= 0;
    }
}