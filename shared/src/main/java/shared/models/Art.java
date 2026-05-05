package shared.models;

import shared.enums.Category;

import java.math.BigDecimal;

// Tác phẩm nghệ thuật
public class Art extends Item {
    private final String artist; // Tên tác giả
    private final int yearCreated; // Năm sản xuất
    private final boolean isOriginal; // Có là bản gốc hay không?

    public Art(String name, String description, Seller seller, BigDecimal startingPrice, String artist, int yearCreated, boolean isOriginal) {
        super(name, description, seller, Category.ARTS, startingPrice);
        this.artist = artist;
        this.yearCreated = yearCreated;
        this.isOriginal = isOriginal;
    }

    public boolean getIsOriginal() {
        return isOriginal;
    }

    public String getArtist() {
        return artist;
    }

    public int getYearCreated() {
        return yearCreated;
    }

    // Kiểm tra thông số có logic không?
    public boolean isValid() {
        return super.isValid() && yearCreated >= 0;
    }
}