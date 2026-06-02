package shared.models.itemfactory;

import shared.enums.ItemStatus;
import shared.models.items.Electronic;
import shared.models.items.Item;
import shared.models.users.Seller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public class ElectronicFactory implements ItemFactory {
    public Item create(ResultSet rs, Seller seller, BigDecimal basePrice) throws SQLException {

        String brand = rs.getString("brand");

        ItemStatus status = ItemStatus.valueOf(
                Optional.ofNullable(rs.getString("item_status"))
                        .orElse("NEW")
        );

        return new Electronic(
                rs.getString("name"),
                rs.getString("description"),
                seller,
                basePrice,
                brand,
                status
        );
    }
    @Override
    public Item create(
            Map<String, String> data,
            Seller seller,
            BigDecimal price) {

        return new Electronic(
                data.get("name"),
                data.get("description"),
                seller,
                price,
                data.getOrDefault("brandField", "Default"),
                ItemStatus.valueOf(
                        data.getOrDefault("statusField", "NEW")
                                .toUpperCase())
        );
    }

    @Override
    public Item create(com.google.gson.JsonObject obj, String name, String description, BigDecimal startingPrice) {
        String brand = obj.get("brand").getAsString();
        ItemStatus status = ItemStatus.valueOf(obj.get("status").getAsString().toUpperCase());
        return new Electronic(name, description, null, startingPrice, brand, status);
    }
}
