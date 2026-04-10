package model;

public class BankInfo {
    private BankList bankName;
    private String accountNumber;

    public BankInfo(BankList bankName, String accountNumber) {
        if (bankName == null || accountNumber == null) {
            throw new IllegalArgumentException();
        }
        this.bankName = bankName;
        this.accountNumber = accountNumber;
    }
    public BankList getBankName() {
        return bankName;
    }
    public String getAccountNumber() {
        return accountNumber;
    }
}