package shared.models;

import java.math.BigDecimal;

// Ví tiền
public class Wallet {
    private BigDecimal balance = BigDecimal.ZERO; // Số dư

    // Nạp tiền
    public synchronized void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException();
        }
        balance = balance.add(amount);
    }

    // Rút tiền
    public synchronized boolean withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException();
        }
        if (balance.compareTo(amount) < 0) {
            return false;
        }
        balance = balance.subtract(amount);
        return true;
    }

    // Chuyển tiền
    public boolean transfer(BigDecimal amount, Seller other) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException();
        }
        Wallet target = other.getWallet();
        Wallet first = System.identityHashCode(this) < System.identityHashCode(target) ? this : target;
        Wallet second = first == this ? target : this;
        synchronized (first) {
            synchronized (second) {
                if (this.balance.compareTo(amount) < 0) {
                    return false;
                }
                this.balance = this.balance.subtract(amount);
                target.balance = target.balance.add(amount);
                return true;
            }
        }
    }

    // Lấy số dư
    public BigDecimal getBalance() {
        return balance;
    }
}