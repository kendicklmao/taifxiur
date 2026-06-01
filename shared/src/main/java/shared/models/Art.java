package shared.models;

import shared.enums.Category;
import shared.models.users.Seller;

import java.math.BigDecimal;
import java.util.Map;
import java.util.LinkedHashMap;

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

    @Override
    public Map<String, String> getAdditionalDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Artist", artist);
        details.put("Year Created", String.valueOf(yearCreated));
        details.put("Original", isOriginal ? "Yes" : "No");
        return details;
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

    public boolean isValid() {
        return super.isValid() && yearCreated >= 0;
    }
}