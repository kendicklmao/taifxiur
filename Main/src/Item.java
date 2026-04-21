public class Item {
    private String id;
    private String name;
    private String description;
    private double basePrice;
    private double currentPrice;
    private String sellerName;
    private boolean legitCheck;
    private static long cnt = 1;
    private Category category;

    public Item(String name, String description, double basePrice, double currentPrice, String sellerName, boolean legitCheck, Category category) {
        this.name = name;
        this.id = String.valueOf(cnt++);
        this.description = description;
        this.basePrice = basePrice;
        this.currentPrice = currentPrice;
        this.sellerName = sellerName;
        this.legitCheck = legitCheck;
        this.category = category;
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

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public boolean isLegitCheck() {
        return legitCheck;
    }

    public void setLegitCheck(boolean legitCheck) {
        this.legitCheck = legitCheck;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

}