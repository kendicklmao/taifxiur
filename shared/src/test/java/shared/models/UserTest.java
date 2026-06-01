package shared.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.enums.Role;
import shared.models.users.Admin;
import shared.models.users.Bidder;
import shared.models.users.Seller;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

public class UserTest {
    private Bidder bidder;
    private Seller seller;
    private Admin admin;

    @BeforeEach
    public void setUp() {
        bidder = new Bidder("bidder123", "Password@123", "bidder123@yahoo.com", "mm bao nhiêu cân?", "100", "mẹ mày tên gì", "béo");
        seller = new Seller("seller123", "Password@123", "seller123@yahoo.com", "mm bao nhiêu cân?", "100", "mẹ mày tên gì", "béo");
        admin = new Admin("admin123", "Password@123", "admin123@yahoo.com", "mm bao nhiêu cân?", "100", "mẹ mày tên gì", "béo");
    }

    @Test
    public void testGetUsername() {
        assertEquals("bidder123", bidder.getUsername());
        assertEquals("seller123", seller.getUsername());
        assertEquals("admin123", admin.getUsername());
    }

    @Test
    public void testGetEmail() {
        assertEquals("bidder123@yahoo.com", bidder.getEmail());
        assertEquals("seller123@yahoo.com", seller.getEmail());
        assertEquals("admin123@yahoo.com", admin.getEmail());
    }

    @Test
    public void testGetRole() {
        assertEquals(Role.BIDDER, bidder.getRole());
        assertEquals(Role.SELLER, seller.getRole());
        assertEquals(Role.ADMIN, admin.getRole());
    }

    @Test
    public void testDefaultNotBanned() {
        assertFalse(bidder.isBanned());
        assertFalse(seller.isBanned());
        assertFalse(admin.isBanned());
    }

    @Test
    public void testBanUser() {
        assertFalse(bidder.isBanned());
        bidder.banUser();
        assertTrue(bidder.isBanned());
    }

    @Test
    public void testUnbanUser() {
        bidder.banUser();
        assertTrue(bidder.isBanned());
        bidder.unbanUser();
        assertFalse(bidder.isBanned());
    }

    @Test
    public void testCheckPasswordCorrect() {
        assertTrue(bidder.checkPassword("Password@123"));
    }

    @Test
    public void testCheckPasswordIncorrect() {
        assertFalse(bidder.checkPassword("WrongPassword@123"));
    }

    @Test
    public void testSetValidEmail() {
        assertTrue(bidder.setEmail("newemail@test.com"));
        assertEquals("newemail@test.com", bidder.getEmail());
    }

    @Test
    public void testSetInvalidEmail() {
        boolean result = bidder.setEmail("invalid-email");
        assertFalse(result);
        assertEquals("bidder123@yahoo.com", bidder.getEmail());
    }

    @Test
    public void testSetValidPassword() {
        assertTrue(bidder.setPassword("NewPassword@456"));
        assertTrue(bidder.checkPassword("NewPassword@456"));
        assertFalse(bidder.checkPassword("Password@123"));
    }

    @Test
    public void testSetInvalidPassword() {
        boolean result = bidder.setPassword("weak");
        assertFalse(result);
        assertTrue(bidder.checkPassword("Password@123"));
    }

    @Test
    public void testChangePasswordSuccess() {
        boolean result = bidder.changePassword("Password@123", "NewPassword@456");
        assertTrue(result);
        assertTrue(bidder.checkPassword("NewPassword@456"));
        assertFalse(bidder.checkPassword("Password@123"));
    }

    @Test
    public void testChangePasswordWrongOldPassword() {
        boolean result = bidder.changePassword("WrongPassword", "NewPassword@456");
        assertFalse(result);
        assertTrue(bidder.checkPassword("Password@123"));
    }

    @Test
    public void testResetPasswordSuccess() {
        assertTrue(bidder.verifySecurityAnswers("100", "béo"));

        boolean result = bidder.resetPassword("100", "béo", "NewPassword@456");
        assertTrue(result);
        assertTrue(bidder.checkPassword("NewPassword@456"));
        assertFalse(bidder.checkPassword("Password@123"));
    }
    @Test
    public void testResetPasswordWrongAnswers() {
        boolean result = bidder.resetPassword("Wrong", "WrongAnswer", "NewPassword@456");
        assertFalse(result);
        assertTrue(bidder.checkPassword("Password@123"));
    }

    @Test
    public void testVerifySecurityAnswersCorrect() {
        boolean result = bidder.verifySecurityAnswers("100", "béo");
        assertTrue(result, "Security answers should match");
    }

    @Test
    public void testVerifySecurityAnswersIncorrect() {
        assertFalse(bidder.verifySecurityAnswers("Fido", "Jane"));
        assertFalse(bidder.verifySecurityAnswers("100", "John"));
        assertFalse(bidder.verifySecurityAnswers("Fido", "John"));
    }

    @Test
    public void testGetSecurityQuestions() {
        assertEquals("mm bao nhiêu cân?", bidder.getSecurityQuestion1());
        assertEquals("mẹ mày tên gì", bidder.getSecurityQuestion2());
    }

    @Test
    public void testBidderHasWallet() {
        assertNotNull(bidder.getWallet());
        assertEquals(0, bidder.getWallet().getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testSellerHasWallet() {
        assertNotNull(seller.getWallet());
        assertEquals(0, seller.getWallet().getBalance().compareTo(BigDecimal.ZERO));
    }
}