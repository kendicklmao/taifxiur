package server.service;

import org.junit.jupiter.api.*;
import server.database.DatabaseConfig;
import server.database.DatabaseInitializer;
import shared.enums.Role;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserServiceTest {

    private static UserService userService;
    private List<String> testUsers;

    @BeforeAll
    public static void setUpClass() throws Exception {
        DatabaseInitializer.initializeDatabase();
        userService = new UserService();
    }

    @BeforeEach
    public void setUp() {
        testUsers = new ArrayList<>();
        userService.initializeDefaultUsers();
    }

    @AfterEach
    public void tearDown() {
        cleanupDatabase();
    }

    @AfterAll
    public static void tearDownClass() {
        DatabaseConfig.closeDataSource();
    }

    private void cleanupDatabase() {
        if (testUsers == null || testUsers.isEmpty()) {
            return;
        }
        try (java.sql.Connection conn = DatabaseConfig.getDataSource().getConnection()) {
            for (String username : testUsers) {
                try (java.sql.PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
                    stmt.setString(1, username);
                    stmt.executeUpdate();
                }
            }
        } catch (java.sql.SQLException e) {}
    }

    @Test
    public void testAdminCannotBanAdmin() {
        String result = userService.banUser("admin", "admin");
        assertEquals("Cannot ban an administrator", result);
    }

    @Test
    public void testNonAdminCannotBanUser() {
        String result = userService.banUser("admin", "bidder");
        assertEquals("Only admin can ban users", result);
    }

    @Test
    public void testRegisterUser() {
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        String testUser = "newuser_" + suffix;
        String testEmail = testUser + "@test.com";

        testUsers.add(testUser);

        boolean result = userService.register(testUser, "Password@123", testEmail, "q", "a", "q", "a", Role.BIDDER);
        assertEquals(true, result, "Registration should succeed for a new unique user");

    }
}