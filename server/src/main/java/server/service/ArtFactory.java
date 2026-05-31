package server.service;

import shared.models.Art;
import shared.models.Item;
import shared.models.Seller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class ArtFactory implements ItemFactory {

    @Override
    public Item create(
            ResultSet rs,
            Seller seller,
            BigDecimal basePrice
    ) throws SQLException {

        return new Art(
                rs.getString("name"),
                rs.getString("description"),
                seller,
                basePrice,
                rs.getString("artist"),
                rs.getInt("year_created"),
                rs.getBoolean("is_original")
        );
    }
    @Override
    public Item create(
            Map<String, String> data,
            Seller seller,
            BigDecimal price) {

        return new Art(
                data.get("name"),
                data.get("description"),
                seller,
                price,
                data.getOrDefault("artistField", "Unknown"),
                Integer.parseInt(
                        data.getOrDefault("yearField", "0")),
                Boolean.parseBoolean(
                        data.getOrDefault("originalBox", "false"))
        );
    }
}
