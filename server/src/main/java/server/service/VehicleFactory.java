package server.service;


import shared.models.Item;
import shared.models.Seller;
import shared.models.Vehicle;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VehicleFactory implements ItemFactory {

    @Override
    public Item create(ResultSet rs, Seller seller, BigDecimal basePrice) throws SQLException {

        return new Vehicle(
                rs.getString("name"),
                rs.getString("description"),
                seller,
                basePrice,
                rs.getString("brand"),
                rs.getInt("model_year"),
                rs.getInt("km_travel")
        );
    }
}
