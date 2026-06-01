package shared.utils;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.models.Admin;
import shared.models.Bidder;
import shared.models.Seller;
import shared.models.User;

import static org.junit.jupiter.api.Assertions.*;

public class GsonUtilsTest {
    private Gson gson;
    private Bidder bidder;
    private Seller seller;
    private Admin admin;

    @BeforeEach
    public void setUp() {
        gson = GsonUtils.createGson();
        bidder = new Bidder(1, "bidder123", "Password@123", "bidder123@yahoo.com", "mm bao nhiêu cân?", "100", "mẹ mày tên gì", "béo");
        seller = new Seller(2, "seller123", "Password@123", "seller123@yahoo.com", "mm bao nhiêu cân?", "100", "mẹ mày tên gì", "béo");
        admin = new Admin(3, "admin123", "Password@123", "admin123@yahoo.com", "mm bao nhiêu cân?", "100", "mẹ mày tên gì", "béo");
    }

    @Test
    public void testSerializeNonBannedUser() {
        String json = gson.toJson(bidder);
        assertTrue(json.contains("\"isBanned\":false"), "JSON should contain isBanned field");
    }

    @Test
    public void testSerializeBannedUser() {
        bidder.banUser();
        String json = gson.toJson(bidder);
        assertTrue(json.contains("\"isBanned\":true"), "JSON should contain isBanned as true");
    }

    @Test
    public void testDeserializeNonBannedUser() {
        String json = gson.toJson(bidder);
        User deserializedUser = gson.fromJson(json, User.class);
        assertFalse(deserializedUser.isBanned(), "Deserialized user should not be banned");
    }

    @Test
    public void testDeserializeBannedUser() {
        bidder.banUser();
        String json = gson.toJson(bidder);
        User deserializedUser = gson.fromJson(json, User.class);
        assertTrue(deserializedUser.isBanned(), "Deserialized user should be banned");
    }

    @Test
    public void testSerializeDeserializeMultipleUsers() {
        seller.banUser();

        String bidderJson = gson.toJson(bidder);
        String sellerJson = gson.toJson(seller);
        String adminJson = gson.toJson(admin);

        User bidderDeserialized = gson.fromJson(bidderJson, User.class);
        User sellerDeserialized = gson.fromJson(sellerJson, User.class);
        User adminDeserialized = gson.fromJson(adminJson, User.class);

        assertFalse(bidderDeserialized.isBanned(), "Bidder should not be banned");
        assertTrue(sellerDeserialized.isBanned(), "Seller should be banned");
        assertFalse(adminDeserialized.isBanned(), "Admin should not be banned");
    }

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

    @Test
    public void testSetBannedMethod() {
        assertFalse(bidder.isBanned(), "User should not be banned initially");
        bidder.setBanned(true);
        assertTrue(bidder.isBanned(), "User should be banned after setBanned(true)");
        bidder.setBanned(false);
        assertFalse(bidder.isBanned(), "User should not be banned after setBanned(false)");
    }
}