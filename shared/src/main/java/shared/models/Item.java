package shared.models;

import shared.enums.Category;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public abstract class Item implements Serializable {//lớp các mặt hàng
    private String id;
    private String name; //tên mặt hàng
    private String description; //mô tả mặt hàng
    private transient Seller seller; //người bán
    private Category category; //loại mặt hàng
    private BigDecimal startingPrice;
    private BigDecimal minIncrement;
    private String imageUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Item(String id, String name, String description, BigDecimal startingPrice, BigDecimal minIncrement, String imageUrl, Seller seller, Category category, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.imageUrl = imageUrl;
        this.seller = seller;
        this.category = category;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Item() {
        // No-arg constructor for deserialization
        this.seller = null;
        this.category = null;
        this.startTime = null;
        this.endTime = null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getMinIncrement() {
        return minIncrement;
    }

    public void setMinIncrement(BigDecimal minIncrement) {
        this.minIncrement = minIncrement;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isValid() { //kiểm tra thông số có logic không ?
        return name != null && name.length() >= 1 && !name.isBlank() &&
               description != null && description.length() >= 1 && !description.isBlank();
    }
}