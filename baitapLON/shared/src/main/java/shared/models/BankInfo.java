package shared.models;

import shared.enums.BankList;

public class BankInfo {
    private BankList bankName;//tên ngân hàng
    private String accountNumber;//số tài khoản

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

