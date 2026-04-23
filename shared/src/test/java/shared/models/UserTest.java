package shared.models;

import org.junit.Before;
import org.junit.Test;
import shared.enums.Role;
import static org.junit.Assert.*;
import java.math.BigDecimal;

/**
 * Unit tests for User class and its subclasses
 */
public class UserTest {
    private Bidder bidder;
    private Seller seller;
    private Admin admin;

    @Before
    public void setUp() {
        bidder = new Bidder("bidder_user", "Password@123", "bidder@test.com",
                            "What is your pet name?", "Fluffy",
                            "What is your mother's name?", "Jane");
        seller = new Seller("seller_user", "Password@123", "seller@test.com",
                           "What is your pet name?", "Fluffy",
                           "What is your mother's name?", "Jane");
        admin = new Admin("admin_user", "Password@123", "admin@test.com",
                         "What is your pet name?", "Fluffy",
                         "What is your mother's name?", "Jane");
    }

    /**
     * Test username getter
     */
    @Test
    public void testGetUsername() {
        assertEquals("bidder_user", bidder.getUsername());
        assertEquals("seller_user", seller.getUsername());
        assertEquals("admin_user", admin.getUsername());
    }

    /**
     * Test email getter
     */
    @Test
    public void testGetEmail() {
        assertEquals("bidder@test.com", bidder.getEmail());
        assertEquals("seller@test.com", seller.getEmail());
        assertEquals("admin@test.com", admin.getEmail());
    }

    /**
     * Test role getter
     */
    @Test
    public void testGetRole() {
        assertEquals(Role.BIDDER, bidder.getRole());
        assertEquals(Role.SELLER, seller.getRole());
        assertEquals(Role.ADMIN, admin.getRole());
    }

    /**
     * Test user is not banned by default
     */
    @Test
    public void testDefaultNotBanned() {
        assertFalse(bidder.isBanned());
        assertFalse(seller.isBanned());
        assertFalse(admin.isBanned());
    }

    /**
     * Test banning user
     */
    @Test
    public void testBanUser() {
        assertFalse(bidder.isBanned());
        bidder.banUser();
        assertTrue(bidder.isBanned());
    }

    /**
     * Test unbanning user
     */
    @Test
    public void testUnbanUser() {
        bidder.banUser();
        assertTrue(bidder.isBanned());
        bidder.unbanUser();
        assertFalse(bidder.isBanned());
    }

    /**
     * Test correct password verification
     */
    @Test
    public void testCheckPasswordCorrect() {
        assertTrue(bidder.checkPassword("Password@123"));
    }

    /**
     * Test incorrect password verification
     */
    @Test
    public void testCheckPasswordIncorrect() {
        assertFalse(bidder.checkPassword("WrongPassword@123"));
    }

    /**
     * Test setting valid email
     */
    @Test
    public void testSetValidEmail() {
        assertTrue(bidder.setEmail("newemail@test.com"));
        assertEquals("newemail@test.com", bidder.getEmail());
    }

    /**
     * Test setting invalid email
     */
    @Test
    public void testSetInvalidEmail() {
        boolean result = bidder.setEmail("invalid-email");
        assertFalse(result);
        // Email should remain unchanged
        assertEquals("bidder@test.com", bidder.getEmail());
    }

    /**
     * Test setting valid password
     */
    @Test
    public void testSetValidPassword() {
        assertTrue(bidder.setPassword("NewPassword@456"));
        assertTrue(bidder.checkPassword("NewPassword@456"));
        assertFalse(bidder.checkPassword("Password@123"));
    }

    /**
     * Test setting invalid password
     */
    @Test
    public void testSetInvalidPassword() {
        boolean result = bidder.setPassword("weak");
        assertFalse(result);
        // Old password should still work
        assertTrue(bidder.checkPassword("Password@123"));
    }

    /**
     * Test changing password with correct old password
     */
    @Test
    public void testChangePasswordSuccess() {
        boolean result = bidder.changePassword("Password@123", "NewPassword@456");
        assertTrue(result);
        assertTrue(bidder.checkPassword("NewPassword@456"));
        assertFalse(bidder.checkPassword("Password@123"));
    }

    /**
     * Test changing password with incorrect old password
     */
    @Test
    public void testChangePasswordWrongOldPassword() {
        boolean result = bidder.changePassword("WrongPassword", "NewPassword@456");
        assertFalse(result);
        assertTrue(bidder.checkPassword("Password@123"));
    }

    /**
     * Test resetting password with correct security answers
     */
    @Test
    public void testResetPasswordSuccess() {
        // First verify that the answers are actually correct before reset
        assertTrue(bidder.verifySecurityAnswers("Fluffy", "Jane"));

        // Now reset password with correct answers
        boolean result = bidder.resetPassword("Fluffy", "Jane", "NewPassword@456");
        assertTrue(result);

        // Verify the new password works
        assertTrue(bidder.checkPassword("NewPassword@456"));

        // Verify old password doesn't work
        assertFalse(bidder.checkPassword("Password@123"));
    }

    /**
     * Test resetting password with incorrect security answers
     */
    @Test
    public void testResetPasswordWrongAnswers() {
        boolean result = bidder.resetPassword("Wrong", "WrongAnswer", "NewPassword@456");
        assertFalse(result);
        assertTrue(bidder.checkPassword("Password@123"));
    }

    /**
     * Test verifying correct security answers
     */
    @Test
    public void testVerifySecurityAnswersCorrect() {
        // Test with exact answers provided during construction
        boolean result = bidder.verifySecurityAnswers("Fluffy", "Jane");
        assertTrue("Security answers should match", result);
    }

    /**
     * Test verifying incorrect security answers
     */
    @Test
    public void testVerifySecurityAnswersIncorrect() {
        assertFalse(bidder.verifySecurityAnswers("Fido", "Jane"));
        assertFalse(bidder.verifySecurityAnswers("Fluffy", "John"));
        assertFalse(bidder.verifySecurityAnswers("Fido", "John"));
    }

    /**
     * Test security questions getter
     */
    @Test
    public void testGetSecurityQuestions() {
        assertEquals("What is your pet name?", bidder.getSecurityQuestion1());
        assertEquals("What is your mother's name?", bidder.getSecurityQuestion2());
    }

    /**
     * Test Bidder has wallet
     */
    @Test
    public void testBidderHasWallet() {
        assertNotNull(bidder.getWallet());
        assertEquals(0, bidder.getWallet().getBalance().compareTo(BigDecimal.ZERO));
    }

    /**
     * Test Seller has wallet
     */
    @Test
    public void testSellerHasWallet() {
        assertNotNull(seller.getWallet());
        assertEquals(0, seller.getWallet().getBalance().compareTo(BigDecimal.ZERO));
    }
}
