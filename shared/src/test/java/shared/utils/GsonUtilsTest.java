package shared.utils;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.models.Admin;
import shared.models.Bidder;
import shared.models.Seller;
import shared.models.User;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests for Gson serialization and deserialization of User objects, especially the isBanned field
public class GsonUtilsTest {
    private Gson gson;
    private Bidder bidder;
    private Seller seller;
    private Admin admin;

    @BeforeEach
    public void setUp() {
        gson = GsonUtils.createGson();
        bidder = new Bidder(1, "bidder_user", "Password@123", "bidder@test.com", "What is your pet name?", "Fluffy",
                           "What is your mother's name?", "Jane");
        seller = new Seller(2, "seller_user", "Password@123", "seller@test.com", "What is your pet name?", "Fluffy",
                           "What is your mother's name?", "Jane");
        admin = new Admin(3, "admin_user", "Password@123", "admin@test.com", "What is your pet name?", "Fluffy",
                         "What is your mother's name?", "Jane");
    }

    //Kiem tra serialization cua user khong bi Ban
    @Test
    public void testSerializeNonBannedUser() {
        String json = gson.toJson(bidder);
        assertTrue(json.contains("\"isBanned\":false"), "JSON should contain isBanned field");
    }

    //Kiem tra serialization cua user bi ban
    @Test
    public void testSerializeBannedUser() {
        bidder.banUser();
        String json = gson.toJson(bidder);
        assertTrue(json.contains("\"isBanned\":true"), "JSON should contain isBanned as true");
    }

    //Kiem tra deserialization cua user khong bi ban
    @Test
    public void testDeserializeNonBannedUser() {
        String json = gson.toJson(bidder);
        User deserializedUser = gson.fromJson(json, User.class);
        assertFalse(deserializedUser.isBanned(), "Deserialized user should not be banned");
    }

    //Kiem tra deserialization cua user bi ban
    @Test
    public void testDeserializeBannedUser() {
        bidder.banUser();
        String json = gson.toJson(bidder);
        User deserializedUser = gson.fromJson(json, User.class);
        assertTrue(deserializedUser.isBanned(), "Deserialized user should be banned");
    }

    //Test voi lop cha User de xac dinh chung chinh la lop con ke thua
    @Test
    public void testSerializeDeserializeMultipleUsers() {
        // Ban only the seller
        seller.banUser();

        String bidderJson = gson.toJson(bidder); // Serialize all three
        String sellerJson = gson.toJson(seller);
        String adminJson = gson.toJson(admin);

        // Deserialize them
        User bidderDeserialized = gson.fromJson(bidderJson, User.class);
        User sellerDeserialized = gson.fromJson(sellerJson, User.class);
        User adminDeserialized = gson.fromJson(adminJson, User.class);

        // Verify ban status
        assertFalse(bidderDeserialized.isBanned(), "Bidder should not be banned");
        assertTrue(sellerDeserialized.isBanned(), "Seller should be banned");
        assertFalse(adminDeserialized.isBanned(), "Admin should not be banned");
    }

    //Test kha nang xu li mang User
    @Test
    public void testSerializeDeserializeUserArray() {
        seller.banUser();
        User[] users = {bidder, seller, admin};

        String json = gson.toJson(users);
        User[] deserializedUsers = gson.fromJson(json, User[].class);

        assertEquals(3, deserializedUsers.length, "Should have 3 users");
        assertFalse(deserializedUsers[0].isBanned(), "First user should not be banned");
        assertTrue(deserializedUsers[1].isBanned(), "Second user should be banned");
        assertFalse(deserializedUsers[2].isBanned(), "Third user should not be banned");
    }

    //Test chuc nang cua method setBanned
    @Test
    public void testSetBannedMethod() {
        assertFalse(bidder.isBanned(), "User should not be banned initially");
        bidder.setBanned(true);
        assertTrue(bidder.isBanned(), "User should be banned after setBanned(true)");
        bidder.setBanned(false);
        assertFalse(bidder.isBanned(), "User should not be banned after setBanned(false)");
    }
}