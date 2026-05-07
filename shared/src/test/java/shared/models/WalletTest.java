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

    //Kiem tra gui tien thanh cong voi so tien duong
    @Test
    public void testDepositSuccess() {
        BigDecimal amount = new BigDecimal("100.00");
        wallet.deposit(amount);
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("100.00")));
    }


    //Kiem tra gui tien nhieu lan va tinh tong tien cua cac lan gui
    @Test
    public void testMultipleDeposits() {
        wallet.deposit(new BigDecimal("100.00"));
        wallet.deposit(new BigDecimal("50.00"));
        wallet.deposit(new BigDecimal("25.50"));
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("175.50")));
    }

    //tien gui la null , check nem exception
    @Test
    public void testDepositNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.deposit(null);
        });
    }

    //tien gui la 0 , check nem exception
    @Test
    public void testDepositZeroAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.deposit(BigDecimal.ZERO);
        });
    }

    //tien gui la am , check nem exception
    @Test
    public void testDepositNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.deposit(new BigDecimal("-50.00"));
        });
    }

    //rut tien va so du duoc cap nhat
    @Test
    public void testWithdrawSuccess() {
        wallet.deposit(new BigDecimal("100.00"));
        boolean result = wallet.withdraw(new BigDecimal("50.00"));
        assertTrue(result);
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("50.00")));
    }

    //rut tien trong truong hop so du khong du
    @Test
    public void testWithdrawInsufficientBalance() {
        wallet.deposit(new BigDecimal("50.00"));
        boolean result = wallet.withdraw(new BigDecimal("100.00"));
        assertFalse(result);
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("50.00")));
    }

    //Rut tien bang 0
    @Test
    public void testWithdrawZeroAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.withdraw(BigDecimal.ZERO);
        });
    }

    //rut tien am
    @Test
    public void testWithdrawNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.withdraw(new BigDecimal("-50.00"));
        });
    }

    //rut tien null
    @Test
    public void testWithdrawNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            wallet.withdraw(null);
        });
    }

    //kiem tra so du ban dau la 0
    @Test
    public void testInitialBalance() {
        assertEquals(0, wallet.getBalance().compareTo(BigDecimal.ZERO));
    }

    //Kiem tra rut het so du, so du con lai la 0
    @Test
    public void testWithdrawExactBalance() {
        BigDecimal amount = new BigDecimal("100.00");
        wallet.deposit(amount);
        boolean result = wallet.withdraw(amount);
        assertTrue(result);
        assertEquals(0, wallet.getBalance().compareTo(BigDecimal.ZERO));
    }
}
