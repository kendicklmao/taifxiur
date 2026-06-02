package shared.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

public class WalletTest {
    private Wallet wallet;

    @BeforeEach
    public void setUp() {
        wallet = new Wallet();
    }

    @Test
    public void testDepositSuccess() {
        BigDecimal amount = new BigDecimal("100.00");
        wallet.deposit(amount);
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("100.00")));
    }

    @Test
    public void testMultipleDeposits() {
        wallet.deposit(new BigDecimal("100.00"));
        wallet.deposit(new BigDecimal("50.00"));
        wallet.deposit(new BigDecimal("25.50"));
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("175.50")));
    }

    @Test
    public void testDepositNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.deposit(null);
        });
    }

    @Test
    public void testDepositZeroAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.deposit(BigDecimal.ZERO);
        });
    }

    @Test
    public void testDepositNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.deposit(new BigDecimal("-50.00"));
        });
    }

    @Test
    public void testWithdrawSuccess() {
        wallet.deposit(new BigDecimal("100.00"));
        boolean result = wallet.withdraw(new BigDecimal("50.00"));
        assertTrue(result);
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("50.00")));
    }

    @Test
    public void testWithdrawInsufficientBalance() {
        wallet.deposit(new BigDecimal("50.00"));
        boolean result = wallet.withdraw(new BigDecimal("100.00"));
        assertFalse(result);
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("50.00")));
    }

    @Test
    public void testWithdrawZeroAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.withdraw(BigDecimal.ZERO);
        });
    }

    @Test
    public void testWithdrawNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.withdraw(new BigDecimal("-50.00"));
        });
    }
    @Test
    public void testWithdrawNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.withdraw(null);
        });
    }

    @Test
    public void testInitialBalance() {
        assertEquals(0, wallet.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testWithdrawExactBalance() {
        BigDecimal amount = new BigDecimal("100.00");
        wallet.deposit(amount);
        boolean result = wallet.withdraw(amount);
        assertTrue(result);
        assertEquals(0, wallet.getBalance().compareTo(BigDecimal.ZERO));
    }
}