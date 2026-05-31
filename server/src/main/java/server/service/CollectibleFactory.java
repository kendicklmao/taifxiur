package server.service;

import shared.models.Collectible;
import shared.models.Item;
import shared.models.Seller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}
