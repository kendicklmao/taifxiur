package shared.models;

import shared.enums.Category;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Lớp các mặt hàng
public abstract class Item implements Serializable {
    private int dbId;
    private String id;
    private String name; // Tên mặt hàng
    private String description; // Mô tả mặt hàng
    private transient Seller seller; // Người bán
    private Category category; // Loại mặt hàng
    private BigDecimal startingPrice;
    private BigDecimal minIncrement;
    private String imageUrl;
    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;

    public Item(String name, String description, Seller seller, Category category) {
        this.name = name;
        this.description = description;
        this.seller = seller;
        this.category = category;
    }

    public Item(String name, String description, Seller seller, Category category, LocalDateTime auctionStartTime, LocalDateTime auctionEndTime) {
        this.name = name;
        this.description = description;
        this.seller = seller;
        this.category = category;
        this.auctionStartTime = auctionStartTime;
        this.auctionEndTime = auctionEndTime;
    }


    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    // No-arg constructor
    public Item() {
        this.seller = null;
        this.category = null;
    }

    public LocalDateTime getAuctionStartTime() {
        return auctionStartTime;
    }

    public void setAuctionStartTime(LocalDateTime auctionStartTime) {
        this.auctionStartTime = auctionStartTime;
    }

    public LocalDateTime getAuctionEndTime() {
        return auctionEndTime;
    }

    public void setAuctionEndTime(LocalDateTime auctionEndTime) {
        this.auctionEndTime = auctionEndTime;
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
        if (minIncrement != null && minIncrement.compareTo(BigDecimal.ZERO) <= 0) {
            this.minIncrement = new BigDecimal("1000"); // Mặc định 1000 nếu số không hợp lệ
        } else {
            this.minIncrement = minIncrement;
        }
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // Kiểm tra thông số có logic không?
    public boolean isValid() {
        return name != null && name.length() >= 1 && !name.isBlank() &&
               description != null && description.length() >= 1 && !description.isBlank() &&
               (minIncrement == null || minIncrement.compareTo(BigDecimal.ZERO) > 0);
    }
}