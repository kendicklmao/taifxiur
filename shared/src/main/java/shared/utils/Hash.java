package shared.utils;

import java.util.UUID;

// Mã hóa mật khẩu và câu trả lời xác nhận danh tính
public class Hash {
    // Công thức mã hóa
    public static String formula(String password, String salt) {
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

    // Tạo 1 string ngẫu nhiên để làm rối loạn mã hóa
    public static String generateSalt() {
        return UUID.randomUUID().toString();
    }
}