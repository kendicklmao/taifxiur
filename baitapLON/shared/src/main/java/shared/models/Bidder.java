package shared.models;

import shared.enums.Role;

public class Bidder extends User { //người đấu giá
    private final Wallet wallet = new Wallet();

    public Bidder(String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(username, password, email, Role.BIDDER, q1, a1, q2, a2);
    }

    public Wallet getWallet() {
        return wallet;
    }
}