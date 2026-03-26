abstract class Item{
    private String id;
    private String name;
    private String description;
    private double basePrice;
    private double currentPrice;
    private String sellerName;
    private static long cnt = 1;
    public Item(String name, String description, double basePrice, double currentPrice, String sellerName){
        this.id = String.valueOf(cnt++);
        this.description = description;
        this.basePrice = basePrice;
        this.currentPrice = currentPrice;
        this.sellerName = sellerName;
    }
}