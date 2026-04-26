package shared.utils;

// Kiểm tra format các input và chuẩn hóa nó
public class Validator {
    // Bỏ trắng và chuyển về chữ thường
    public static String normalizeAndLowercase(String text) {
        if (text == null) {
            return null;
        }
        return text.trim().toLowerCase();
    }

    // Bỏ trắng nhưng giữ nguyên chữ hoa thường
    public static String normalize(String text) {
        if (text == null) {
            return null;
        }
        return text.trim();
    }

    // Kiểm tra format tên tài khoản
    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        username = normalizeAndLowercase(username);
        return username.length() >= 6 && !username.contains(" ") && username.matches("^[a-zA-Z0-9_]+$");
    }

    // Kiểm tra format mật khẩu
    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        password = normalize(password);
        return password.length() >= 6 && !password.contains(" ") &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    // Kiểm tra format email
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        email = normalizeAndLowercase(email);
        return !email.contains(" ") &&
                email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    // Kiểm tra format câu hỏi xác nhận danh tính
    public static boolean isValidQuestion(String question) {
        if (question == null) {
            return false;
        }
        return !normalize(question).isEmpty();
    }

    // Kiểm tra format câu trả lời xác nhận danh tính
    public static boolean isValidAnswer(String answer) {
        if (answer == null) {
            return false;
        }
        return !normalizeAndLowercase(answer).isEmpty();
    }
}
