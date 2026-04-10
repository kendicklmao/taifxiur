package model;

import java.math.BigDecimal;

public class Fashion extends Item {
    private String brand; //hãng
    private ItemStatus status; //trạng thái mặt hàng: new || like new || used

    public Fashion(String name, String description, Seller seller, String brand, ItemStatus status){
        super(name, description, seller, Category.FASHIONS);
        this.brand = brand;
        this.status = status;
    }
    public String getBrand(){
        return brand;
    }
    public ItemStatus getStatus(){
        return status;
    }
    public BigDecimal getMinIncrement() { //số tiền lớn hơn tối thiểu so với giá hiện tại là 100k
        return new BigDecimal("100000");
    }
    public boolean isValid(){
        return true;
    }
}