package shared.models;

import shared.enums.Category;

public class Art extends Item { //tác phẩm nghệ thuật
    private final String artist; //tên tác giả
    private final int yearCreated; //năm sản xuất
    private final boolean isOriginal; //có là bản gốc hay không?

    public Art(String name, String description, Seller seller, String artist, int yearCreated, boolean isOriginal) {
        super(name, description, seller, Category.ARTS, null, null);
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



    public boolean isValid() { //kiểm tra thông số có logic không?
        return super.isValid() && yearCreated >= 0;
    }
}