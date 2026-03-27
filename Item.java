public class Item {
    private String name;
    private String description;
    private String sellerName;
    private Category category;

    public Item(String name, String description, String sellerName, Category category) {
        this.name = name;
        this.description = description;
        this.sellerName = sellerName;
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
    public String getSellerName() {
        return sellerName;
    }
    public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }
}