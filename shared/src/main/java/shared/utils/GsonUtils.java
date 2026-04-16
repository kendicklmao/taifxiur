package shared.utils;

import com.google.gson.*;
import shared.enums.Category;
import shared.enums.ItemStatus;
import shared.models.*;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

public class GsonUtils {
    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .registerTypeAdapter(Item.class, new ItemDeserializer())
                .registerTypeAdapter(ScheduledFuture.class, new ScheduledFutureAdapter())
                .create();
    }

    private static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Instant.parse(json.getAsString());
        }
    }

    private static class ItemDeserializer implements JsonDeserializer<Item> {
        @Override
        public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            Category category = Category.valueOf(obj.get("category").getAsString());
            String name = obj.get("name").getAsString();
            String description = obj.get("description").getAsString();
            // seller is null
            switch (category) {
                case COLLECTIBLES:
                    int yearCreated = obj.get("yearCreated").getAsInt();
                    return new Collectible(name, description, null, yearCreated);
                case ELECTRONICS: {
                    String brand = obj.get("brand").getAsString();
                    ItemStatus status = ItemStatus.valueOf(obj.get("status").getAsString().toUpperCase());
                    return new Electronic(name, description, null, brand, status);
                }
                case ARTS: {
                    String artist = obj.get("artist").getAsString();
                    int year = obj.get("yearCreated").getAsInt();
                    boolean original = obj.get("isOriginal").getAsBoolean();
                    return new Art(name, description, null, artist, year, original);
                }
                case VEHICLES: {
                    String brand = obj.get("brand").getAsString();
                    int model = obj.get("model").getAsInt();
                    int km = obj.get("kmTravel").getAsInt();
                    return new Vehicle(name, description, null, brand, model, km);
                }
                case FASHIONS: {
                    String brand = obj.get("brand").getAsString();
                    ItemStatus status = ItemStatus.valueOf(obj.get("status").getAsString().toUpperCase());
                    return new Fashion(name, description, null, brand, status);
                }
                default:
                    throw new JsonParseException("Unknown category: " + category);
            }
        }
    }

    private static class ScheduledFutureAdapter implements JsonSerializer<ScheduledFuture<?>>, JsonDeserializer<ScheduledFuture<?>> {
        @Override
        public JsonElement serialize(ScheduledFuture<?> src, Type typeOfSrc, JsonSerializationContext context) {
            return JsonNull.INSTANCE; // or new JsonObject()
        }

        @Override
        public ScheduledFuture<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return null;
        }
    }
}
