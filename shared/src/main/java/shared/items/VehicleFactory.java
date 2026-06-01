package shared.items;


import shared.models.Item;
import shared.models.Vehicle;
import shared.models.users.Seller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

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
    @Override
    public Item create(
            Map<String, String> data,
            Seller seller,
            BigDecimal price) {

        return new Vehicle(
                data.get("name"),
                data.get("description"),
                seller,
                price,
                data.getOrDefault("brandField", "Unknown"),
                Integer.parseInt(
                        data.getOrDefault("modelField", "0")),
                Integer.parseInt(
                        data.getOrDefault("kmField", "0"))
        );
    }

    @Override
    public Item create(com.google.gson.JsonObject obj, String name, String description, BigDecimal startingPrice) {
        String brand = obj.get("brand").getAsString();
        int model = obj.get("model").getAsInt();
        int km = obj.get("kmTravel").getAsInt();
        return new Vehicle(name, description, null, startingPrice, brand, model, km);
    }
}
