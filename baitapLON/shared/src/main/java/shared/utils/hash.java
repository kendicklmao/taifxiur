package shared.utils;

import java.util.UUID;
public class hash { //mã hóa
    public static String formula(String password, String salt) {//công thức mã hóa
        String combined = password + salt;
        long res = 7;
        for (int round = 0; round < 101; round++) {
            for (int i = 0; i < combined.length(); i++) {
                res = res * 131 + combined.charAt(i);
                res %= 998244353;
            }
            combined = String.valueOf(res);
        }
        return String.valueOf(res);
    }
    public static String generateSalt() { //random 1 String ngẫu nhiên để làm rối loạn mã hóa
        return UUID.randomUUID().toString();
    }
}