package shared.utils;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import shared.models.Admin;
import shared.models.Bidder;
import shared.models.Seller;
import shared.models.User;

import static org.junit.Assert.*;

/**
 * Unit tests for Gson serialization and deserialization of User objects,
 * especially the isBanned field
 */
public class GsonUtilsTest {
    private Gson gson;
    private Bidder bidder;
    private Seller seller;
    private Admin admin;

    @Before
    public void setUp() {
        gson = GsonUtils.createGson();
        bidder = new Bidder(1, "bidder_user", "Password@123", "bidder@test.com",
                           "What is your pet name?", "Fluffy",
                           "What is your mother's name?", "Jane");
        seller = new Seller(2, "seller_user", "Password@123", "seller@test.com",
                           "What is your pet name?", "Fluffy",
                           "What is your mother's name?", "Jane");
        admin = new Admin(3, "admin_user", "Password@123", "admin@test.com",
                         "What is your pet name?", "Fluffy",
                         "What is your mother's name?", "Jane");
    }

    /**
     * Test serializing a non-banned user
     */
    @Test
    public void testSerializeNonBannedUser() {
        String json = gson.toJson(bidder);
        assertTrue("JSON should contain isBanned field", json.contains("\"isBanned\":false"));
    }

    /**
     * Test serializing a banned user
     */
    @Test
    public void testSerializeBannedUser() {
        bidder.banUser();
        String json = gson.toJson(bidder);
        assertTrue("JSON should contain isBanned as true", json.contains("\"isBanned\":true"));
    }

    /**
     * Test deserializing a non-banned user
     */
    @Test
    public void testDeserializeNonBannedUser() {
        String json = gson.toJson(bidder);
        User deserializedUser = gson.fromJson(json, User.class);
        assertFalse("Deserialized user should not be banned", deserializedUser.isBanned());
    }

    /**
     * Test deserializing a banned user
     */
    @Test
    public void testDeserializeBannedUser() {
        bidder.banUser();
        String json = gson.toJson(bidder);
        User deserializedUser = gson.fromJson(json, User.class);
        assertTrue("Deserialized user should be banned", deserializedUser.isBanned());
    }

    /**
     * Test serializing and deserializing multiple users with different ban statuses
     */
    @Test
    public void testSerializeDeserializeMultipleUsers() {
        // Ban only the seller
        seller.banUser();

        // Serialize all three
        String bidderJson = gson.toJson(bidder);
        String sellerJson = gson.toJson(seller);
        String adminJson = gson.toJson(admin);

        // Deserialize them
        User bidderDeserialized = gson.fromJson(bidderJson, User.class);
        User sellerDeserialized = gson.fromJson(sellerJson, User.class);
        User adminDeserialized = gson.fromJson(adminJson, User.class);

        // Verify ban status
        assertFalse("Bidder should not be banned", bidderDeserialized.isBanned());
        assertTrue("Seller should be banned", sellerDeserialized.isBanned());
        assertFalse("Admin should not be banned", adminDeserialized.isBanned());
    }

    /**
     * Test serializing and deserializing array of users
     */
    @Test
    public void testSerializeDeserializeUserArray() {
        seller.banUser();
        User[] users = {bidder, seller, admin};

        String json = gson.toJson(users);
        User[] deserializedUsers = gson.fromJson(json, User[].class);

        assertEquals("Should have 3 users", 3, deserializedUsers.length);
        assertFalse("First user should not be banned", deserializedUsers[0].isBanned());
        assertTrue("Second user should be banned", deserializedUsers[1].isBanned());
        assertFalse("Third user should not be banned", deserializedUsers[2].isBanned());
    }

    /**
     * Test that setBanned method works correctly
     */
    @Test
    public void testSetBannedMethod() {
        assertFalse("User should not be banned initially", bidder.isBanned());
        bidder.setBanned(true);
        assertTrue("User should be banned after setBanned(true)", bidder.isBanned());
        bidder.setBanned(false);
        assertFalse("User should not be banned after setBanned(false)", bidder.isBanned());
    }
}

