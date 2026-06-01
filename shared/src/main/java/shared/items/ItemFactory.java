package shared.items;

import com.google.gson.JsonObject;
import shared.models.Item;
import shared.models.users.Seller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public interface ItemFactory {
    Item create(ResultSet rs, Seller seller, BigDecimal basePrice) throws SQLException;
    Item create(Map<String,String> data, Seller seller, BigDecimal price) throws SQLException;
    Item create(JsonObject obj, String name, String description, BigDecimal startingPrice);
}
