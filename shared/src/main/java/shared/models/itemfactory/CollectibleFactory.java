package shared.models.itemfactory;

import shared.models.items.Collectible;
import shared.models.items.Item;
import shared.models.users.Seller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class CollectibleFactory implements ItemFactory {

    @Override
    public Item create(
            ResultSet rs,
            Seller seller,
            BigDecimal basePrice
    ) throws SQLException {

        return new Collectible(
                rs.getString("name"),
                rs.getString("description"),
                seller,
                basePrice,
                rs.getInt("year_created")
        );
    }
    @Override
    public Item create(
            Map<String, String> data,
            Seller seller,
            BigDecimal price) {

        return new Collectible(
                data.get("name"),
                data.get("description"),
                seller,
                price,
                Integer.parseInt(
                        data.getOrDefault("yearField", "0"))
        );
    }

    @Override
    public Item create(com.google.gson.JsonObject obj, String name, String description, BigDecimal startingPrice) {
        int yearCreated = obj.get("yearCreated").getAsInt();
        return new Collectible(name, description, null, startingPrice, yearCreated);
    }
}
