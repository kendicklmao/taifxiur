package shared.utils;

import com.google.gson.*;
import shared.enums.Category;
import shared.enums.ItemStatus;
import shared.models.*;

import java.lang.reflect.Type;
import java.math.BigDecimal;
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
                .registerTypeAdapter(User.class, new UserSerializer())
                .registerTypeAdapter(User.class, new UserDeserializer())
                .registerTypeAdapter(ScheduledFuture.class, new ScheduledFutureAdapter())
                .create();
    }

    private static class UserSerializer implements JsonSerializer<User> {
        @Override
        public JsonElement serialize(User src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", src.getId());
            obj.addProperty("username", src.getUsername());
            obj.addProperty("email", src.getEmail());
            obj.addProperty("role", src.getRole().toString());
            obj.addProperty("isBanned", src.isBanned());
            obj.addProperty("securityQuestion1", src.getSecurityQuestion1());
            obj.addProperty("securityQuestion2", src.getSecurityQuestion2());
            obj.addProperty("securityAnswer1", src.getSecurityQuestion1()); // Use question for consistency
            obj.addProperty("securityAnswer2", src.getSecurityQuestion2()); // Use question for consistency
            return obj;
        }
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
            boolean isBanned = obj.has("isBanned") && obj.get("isBanned").getAsBoolean();

            User user = switch (role) {
                case "ADMIN" -> new Admin(id, username, "", email, q1, a1, q2, a2);
                case "BIDDER" -> new Bidder(id, username, "", email, q1, a1, q2, a2);
                case "SELLER" -> new Seller(id, username, "", email, q1, a1, q2, a2);
                default -> throw new JsonParseException("Unknown user role: " + role);
            };

            if (isBanned) {
                user.setBanned(true);
            }

            return user;
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
            BigDecimal startingPrice = obj.has("startingPrice") ? obj.get("startingPrice").getAsBigDecimal() : BigDecimal.ZERO;

            BigDecimal minIncrement = obj.has("minIncrement") ? obj.get("minIncrement").getAsBigDecimal() : BigDecimal.ZERO;
            String imageUrl = obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : null;
            
            Item item = switch (category) {
                case COLLECTIBLES -> {
                    int yearCreated = obj.get("yearCreated").getAsInt();
                    yield new Collectible(name, description, null, startingPrice, yearCreated);
                }
                case ELECTRONICS -> {
                    String brand = obj.get("brand").getAsString();
                    ItemStatus status = ItemStatus.valueOf(obj.get("status").getAsString().toUpperCase());
                    yield new Electronic(name, description, null, startingPrice, brand, status, null, null);
                }
                case ARTS -> {
                    String artist = obj.get("artist").getAsString();
                    int year = obj.get("yearCreated").getAsInt();
                    boolean original = obj.get("isOriginal").getAsBoolean();
                    yield new Art(name, description, null, startingPrice, artist, year, original);
                }
                case VEHICLES -> {
                    String brand = obj.get("brand").getAsString();
                    int model = obj.get("model").getAsInt();
                    int km = obj.get("kmTravel").getAsInt();
                    yield new Vehicle(name, description, null, startingPrice, brand, model, km);
                }
                case FASHIONS -> {
                    String brand = obj.get("brand").getAsString();
                    ItemStatus status = ItemStatus.valueOf(obj.get("status").getAsString().toUpperCase());
                    yield new Fashion(name, description, null, startingPrice, brand, status);
                }
                default -> throw new JsonParseException("Unknown category: " + category);
            };
            
            item.setMinIncrement(minIncrement);
            item.setImageUrl(imageUrl);
            return item;
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
