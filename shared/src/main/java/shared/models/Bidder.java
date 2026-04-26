package shared.models;

import shared.enums.Role;

// Người đấu giá
public class Bidder extends User {
    public Bidder(int id, String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(id, username, password, email, Role.BIDDER, q1, a1, q2, a2);
    }

    public Bidder(String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(0, username, password, email, Role.BIDDER, q1, a1, q2, a2);
    }

    public Bidder(int id, String username, String hashedPassword, String passwordSalt, String email, 
                  boolean isBanned, String q1, String hashedA1, String saltA1, String q2, String hashedA2, String saltA2) {
        super(id, username, hashedPassword, passwordSalt, email, Role.BIDDER, isBanned, q1, hashedA1, saltA1, q2, hashedA2, saltA2);
    }
}