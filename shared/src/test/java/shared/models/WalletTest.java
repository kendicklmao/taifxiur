package shared.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

/**
 * Unit tests for Wallet class
 */
public class WalletTest {
    private Wallet wallet;

    @BeforeEach
    public void setUp() {
        wallet = new Wallet();
    }

    /**
     * Test successful deposit with positive amount
     */
    @Test
    public void testDepositSuccess() {
        BigDecimal amount = new BigDecimal("100.00");
        wallet.deposit(amount);
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("100.00")));
    }

    /**
     * Test multiple deposits
     */
    @Test
    public void testMultipleDeposits() {
        wallet.deposit(new BigDecimal("100.00"));
        wallet.deposit(new BigDecimal("50.00"));
        wallet.deposit(new BigDecimal("25.50"));
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("175.50")));
    }

    /**
     * Test deposit with null amount throws exception
     */
    @Test
    public void testDepositNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.deposit(null);
        });
    }

    /**
     * Test deposit with zero amount throws exception
     */
    @Test
    public void testDepositZeroAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.deposit(BigDecimal.ZERO);
        });
    }

    /**
     * Test deposit with negative amount throws exception
     */
    @Test
    public void testDepositNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.deposit(new BigDecimal("-50.00"));
        });
    }

    /**
     * Test successful withdrawal
     */
    @Test
    public void testWithdrawSuccess() {
        wallet.deposit(new BigDecimal("100.00"));
        boolean result = wallet.withdraw(new BigDecimal("50.00"));
        assertTrue(result);
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("50.00")));
    }

    /**
     * Test withdrawal with insufficient balance
     */
    @Test
    public void testWithdrawInsufficientBalance() {
        wallet.deposit(new BigDecimal("50.00"));
        boolean result = wallet.withdraw(new BigDecimal("100.00"));
        assertFalse(result);
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("50.00")));
    }

    /**
     * Test withdrawal with zero amount throws exception
     */
    @Test
    public void testWithdrawZeroAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.withdraw(BigDecimal.ZERO);
        });
    }

    /**
     * Test withdrawal with negative amount throws exception
     */
    @Test
    public void testWithdrawNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.withdraw(new BigDecimal("-50.00"));
        });
    }

    /**
     * Test withdrawal with null amount throws exception
     */
    @Test
    public void testWithdrawNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.withdraw(null);
        });
    }

    /**
     * Test initial balance is zero
     */
    @Test
    public void testInitialBalance() {
        assertEquals(0, wallet.getBalance().compareTo(BigDecimal.ZERO));
    }

    /**
     * Test withdraw exact balance
     */
    @Test
    public void testWithdrawExactBalance() {
        BigDecimal amount = new BigDecimal("100.00");
        wallet.deposit(amount);
        boolean result = wallet.withdraw(amount);
        assertTrue(result);
        assertEquals(0, wallet.getBalance().compareTo(BigDecimal.ZERO));
    }
}
