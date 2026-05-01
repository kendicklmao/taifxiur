package shared.models;

import shared.enums.Category;

import java.math.BigDecimal;

public class Collectible extends Item { //mặt hàng sưu tập
    private int yearCreated;

    public Collectible(String name, String description, Seller seller, BigDecimal startingPrice, int yearCreated) {
        super(name, description, seller, Category.COLLECTIBLES, startingPrice, null, null);
        this.yearCreated = yearCreated;
    }


    public int getYearCreated() {
        return yearCreated;
    }

    public boolean isValid() { //kiểm tra thông số có logic không?
        return super.isValid() && yearCreated >= 0;
    }
}