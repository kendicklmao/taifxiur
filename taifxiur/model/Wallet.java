package model;

import java.math.BigDecimal;

public class Wallet {
    private BigDecimal balance = BigDecimal.ZERO;

    public synchronized void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException();
        }
        balance = balance.add(amount);
    }
    public synchronized boolean withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException();
        }
        if (balance.compareTo(amount) < 0){
            return false;
        }
        balance = balance.subtract(amount);
        return true;
    }
    public boolean transfer(BigDecimal amount, Seller other) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException();
        }
        Wallet target = other.getWallet();
        Wallet first = this;
        Wallet second = target;
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
    public BigDecimal getBalance() {
        return balance;
    }
}