package shared.models;

import shared.enums.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Art extends Item { //tác phẩm nghệ thuật
    private final String artist; //tên tác giả
    private final int yearCreated; //năm sản xuất
    private final boolean isOriginal; //có là bản gốc hay không?

    public Art(String name, String description, Seller seller, String artist, int yearCreated, boolean isOriginal, LocalDateTime startTime, LocalDateTime endTime) {
        super(null, name, description, null, null, null, seller, Category.ARTS, startTime, endTime);
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

    public BigDecimal getMinIncrement() { //số tiền lớn hơn tối thiểu so với giá hiện tại là 10tr
        return new BigDecimal("10000000");
    }

    public boolean isValid() { //kiểm tra thông số có logic không?
        return super.isValid() && yearCreated >= 0;
    }
}