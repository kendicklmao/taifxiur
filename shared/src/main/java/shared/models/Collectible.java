package shared.models;

import shared.enums.Category;

import java.math.BigDecimal;

public class Collectible extends Item { //mặt hàng sưu tập
    private int yearCreated;

    public Collectible(String name, String description, Seller seller, int yearCreated) {
        super(name, description, seller, Category.COLLECTIBLES);
        this.yearCreated = yearCreated;
    }

    public Collectible() {
        // No-arg constructor for deserialization
        super();
        this.yearCreated = 0;
    }

    public int getYearCreated() {
        return yearCreated;
    }

    public BigDecimal getMinIncrement() { //số tiền lớn hơn tối thiểu so với giá hiện tại là 1tr
        return new BigDecimal("1000000");
    }

    public boolean isValid() { //kiểm tra thông số có logic không?
        return super.isValid() && yearCreated >= 0;
    }
}