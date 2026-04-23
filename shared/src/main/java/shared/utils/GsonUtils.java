package shared.utils;

import com.google.gson.*;
import shared.enums.Category;
import shared.enums.ItemStatus;
import shared.models.*;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;

public class GsonUtils {
    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(Item.class, new ItemDeserializer())
                .registerTypeAdapter(User.class, new UserDeserializer())
                .registerTypeAdapter(ScheduledFuture.class, new ScheduledFutureAdapter())
                .create();
    }

    private static class UserDeserializer implements JsonDeserializer<User> {
        @Override
        public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String role = obj.get("role").getAsString();
            int id = obj.get("id").getAsInt();
            String username = obj.get("username").getAsString();
            String email = obj.get("email").getAsString();
            String q1 = obj.get("securityQuestion1").getAsString();
            String a1 = obj.get("securityAnswer1").getAsString();
            String q2 = obj.get("securityQuestion2").getAsString();
            String a2 = obj.get("securityAnswer2").getAsString();

            switch (role) {
                case "ADMIN":
                    return new Admin(id, username, "", email, q1, a1, q2, a2);
                case "BIDDER":
                    return new Bidder(id, username, "", email, q1, a1, q2, a2);
                case "SELLER":
                    return new Seller(id, username, "", email, q1, a1, q2, a2);
                default:
                    throw new JsonParseException("Unknown user role: " + role);
            }
        }
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

    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(formatter.format(src));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(json.getAsString(), formatter);
        }
    }

    private static class ItemDeserializer implements JsonDeserializer<Item> {
        @Override
        public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            Category category = Category.valueOf(obj.get("category").getAsString());
            String name = obj.get("name").getAsString();
            String description = obj.get("description").getAsString();
            LocalDateTime startTime = null;
            if (obj.has("startTime") && !obj.get("startTime").isJsonNull()) {
                startTime = context.deserialize(obj.get("startTime"), LocalDateTime.class);
            }

            LocalDateTime endTime = null;
            if (obj.has("endTime") && !obj.get("endTime").isJsonNull()) {
                endTime = context.deserialize(obj.get("endTime"), LocalDateTime.class);
            }
            // seller is null
            switch (category) {
                case COLLECTIBLES:
                    int yearCreated = obj.get("yearCreated").getAsInt();
                    return new Collectible(name, description, null, yearCreated, startTime, endTime);
                case ELECTRONICS: {
                    String brand = obj.get("brand").getAsString();
                    ItemStatus status = ItemStatus.valueOf(obj.get("status").getAsString().toUpperCase());
                    return new Electronic(name, description, null, brand, status, startTime, endTime);
                }
                case ARTS: {
                    String artist = obj.get("artist").getAsString();
                    int year = obj.get("yearCreated").getAsInt();
                    boolean original = obj.get("isOriginal").getAsBoolean();
                    return new Art(name, description, null, artist, year, original, startTime, endTime);
                }
                case VEHICLES: {
                    String brand = obj.get("brand").getAsString();
                    int model = obj.get("model").getAsInt();
                    int km = obj.get("kmTravel").getAsInt();
                    return new Vehicle(name, description, null, brand, model, km, startTime, endTime);
                }
                case FASHIONS: {
                    String brand = obj.get("brand").getAsString();
                    ItemStatus status = ItemStatus.valueOf(obj.get("status").getAsString().toUpperCase());
                    return new Fashion(name, description, null, brand, status, startTime, endTime);
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
