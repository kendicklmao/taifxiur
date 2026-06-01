package shared.models.itemfactory;

import shared.enums.ItemStatus;
import shared.models.items.Fashion;
import shared.models.items.Item;
import shared.models.users.Seller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public class FashionFactory implements ItemFactory {

    @Override
    public Item create(
            ResultSet rs,
            Seller seller,
            BigDecimal basePrice
    ) throws SQLException {

        ItemStatus status = ItemStatus.valueOf(
                Optional.ofNullable(rs.getString("item_status"))
                        .orElse("NEW")
        );

        return new Fashion(
                rs.getString("name"),
                rs.getString("description"),
                seller,
                basePrice,
                rs.getString("brand"),
                status
        );
    }
    @Override
    public Item create(
            Map<String, String> data,
            Seller seller,
            BigDecimal price) {

        return new Fashion(
                data.get("name"),
                data.get("description"),
                seller,
                price,
                data.getOrDefault("brandField", "Brand"),
                ItemStatus.valueOf(
                        data.getOrDefault("statusField", "NEW")
                                .toUpperCase())
        );
    }

    @Override
    public Item create(com.google.gson.JsonObject obj, String name, String description, BigDecimal startingPrice) {
        String brand = obj.get("brand").getAsString();
        ItemStatus status = ItemStatus.valueOf(obj.get("status").getAsString().toUpperCase());
        return new Fashion(name, description, null, startingPrice, brand, status);
    }
}
