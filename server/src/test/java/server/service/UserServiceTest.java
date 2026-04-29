package server.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import server.database.DatabaseConfig;
import shared.enums.Role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UserServiceTest {

    private UserService userService;

    @Before
    public void setUp() {
        userService = new UserService();
        userService.initializeDefaultUsers();
    }

    @After
    public void tearDown() {
        DatabaseConfig.closeDataSource();
    }

    @Test
    public void testAdminCannotBanAdmin() {
        // Admin1 tries to ban Admin2
        String result = userService.banUser("admin1", "admin");
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
        assertNotNull(userService.login("bidder", "Admin@123"));
    }
}
