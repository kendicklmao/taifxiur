package server.service;

import shared.models.Collectible;
import shared.models.Item;
import shared.models.Seller;

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
}
