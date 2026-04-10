package shared.models;

import shared.enums.Category;

import java.math.BigDecimal;

public abstract class Item {//lớp các mặt hàng
    private String name; //tên mặt hàng
    private String description; //mô tả mặt hàng
    private final Seller seller; //người bán
    private Category category; //loại mặt hàng

    public Item(String name, String description, Seller seller, Category category) {
        this.name = name;
        this.description = description;
        this.seller = seller;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Seller getSeller() {
        return seller;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public abstract BigDecimal getMinIncrement();//lấy giá tăng tối thiểu

    public abstract boolean isValid(); //kiểm tra thông số có logic không ?
}