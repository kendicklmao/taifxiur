package model;

import java.math.BigDecimal;

public class Art extends Item { //tác phẩm nghệ thuật
    private String artist; //tên tác giả
    private int yearCreated; //năm sản xuất
    private boolean isOriginal; //có là bản gốc hay không?

    public Art (String name, String description, Seller seller, String artist, int yearCreated, boolean isOriginal){
        super(name, description, seller, Category.ARTS);
        this.artist = artist;
        this.yearCreated = yearCreated;
        this.isOriginal = isOriginal;
    }
    public boolean isOriginal() {
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
    public boolean isValid(){ //kiểm tra thông số có logic không?
        if (yearCreated < 0){
            return false;
        }
        return true;
    }
}