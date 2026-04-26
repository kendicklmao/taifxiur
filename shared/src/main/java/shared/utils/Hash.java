package shared.utils;

import java.util.UUID;

// MÃ HÓA MẬT KHẨU VÀ CÂU TRẢ LỜI XÁC NHẬN DANH TÍNH
public class Hash {
    public static String formula(String password, String salt) {
        // Công thức mã hóa
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

    // Random 1 String ngẫu nhiên để làm rối loạn mã hóa
    public static String generateSalt() {
        return UUID.randomUUID().toString();
    }
}