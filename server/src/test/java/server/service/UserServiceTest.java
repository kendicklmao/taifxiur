package server.service;

import org.junit.jupiter.api.*;
import server.database.DatabaseConfig;
import server.database.DatabaseInitializer;
import shared.enums.Role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserServiceTest {

    private static UserService userService;
    private static WalletService walletService;

    @BeforeAll
    public static void setUpClass() throws Exception {
        DatabaseInitializer.initializeDatabase();
        userService = new UserService();
        walletService = new WalletService();
        userService.setWalletService(walletService);
    }

    @BeforeEach
    public void setUp() {
        cleanupDatabase();
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
        // FIXME: Commented out to prevent wiping the actual database during tests
        // try (java.sql.Connection conn = DatabaseConfig.getDataSource().getConnection();
        //      java.sql.Statement stmt = conn.createStatement()) {
        //     stmt.executeUpdate("DELETE FROM wallets");
        //     stmt.executeUpdate("DELETE FROM users");
        // } catch (java.sql.SQLException e) {
        //     e.printStackTrace();
        // }
    }

    @Test
    public void testAdminCannotBanAdmin() {
        // Admin1 tries to ban Admin2
        String result = userService.banUser("admin", "admin");
        // Check that the result is the expected error message
        assertEquals("Cannot ban an administrator", result);
    }

    @Test
    public void testNonAdminCannotBanUser() {
        // A bidder tries to ban an admin
        String result = userService.banUser("admin", "bidder");
        // Check that the result is the expected error message
        assertEquals("Only admin can ban users", result);
    }

    @Test
    public void testRegisterUser() {
        boolean result = userService.register("newuser", "Password@123", "newuser@test.com", "q", "a", "q", "a", Role.BIDDER);
        assertEquals(true, result);
    }

    @Test
    public void testLoginUser() {
        assertNotNull(userService.login("bidder", "Bidder@123"));
    }
}
