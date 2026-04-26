package server.service;

import org.junit.Test;
import shared.enums.Role;
import static org.junit.Assert.assertEquals;

public class UserServiceTest {

    @Test
    public void testAdminCannotBanAdmin() {
        UserService userService = new UserService();
        
        // Giả sử có 2 admin là admin1 và admin2
        userService.register("admin1", "Pass@123", "1@gmail.com", "q", "a", "q", "a", Role.ADMIN);
        userService.register("admin2", "Pass@123", "2@gmail.com", "q", "a", "q", "a", Role.ADMIN);
        
        // Admin1 cố gắng ban Admin2
        String result = userService.banUser("admin2", "admin1");
        
        // Kiểm tra kết quả phải trả về lỗi chặn
        assertEquals("Cannot ban an administrator", result);
    }

    @Test
    public void testNonAdminCannotBanUser() {
        UserService userService = new UserService();
        
        userService.register("admin1", "Pass@123", "1@gmail.com", "q", "a", "q", "a", Role.ADMIN);
        userService.register("bidder1", "Pass@123", "3@gmail.com", "q", "a", "q", "a", Role.BIDDER);
        
        // Bidder cố gắng ban 1 người dùng khác
        String result = userService.banUser("admin1", "bidder1");
        
        // Kiểm tra kết quả
        assertEquals("Only admin can ban users", result);
    }
}
