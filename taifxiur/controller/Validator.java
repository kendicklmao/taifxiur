package controller;

public class Validator { //kiểm tra format các input và chuẩn hóa nó
    public static String normalizeUsername(String username) {//chuẩn hóa tên tài khoản
        if (username == null) return null;
        return username.trim().toLowerCase();
    }
    public static String normalizePassword(String password) { //chuẩn hóa mật khẩu
        if (password == null) return null;
        return password.trim();
    }
    public static String normalizeEmail(String email) {//chuẩn hóa email
        if (email == null) return null;
        return email.trim().toLowerCase();
    }
    public static String normalizeQuestion(String question) {//chuẩn hóa câu hỏi xác nhận danh tính
        if (question == null) return null;
        return question.trim();
    }
    public static String normalizeAnswer(String answer) {//chuẩn hóa câu trả lời xác nhận danh tính
        if (answer == null) return null;
        return answer.trim().toLowerCase();
    }
    public static boolean isValidUsername(String username) {//kểm tra format tên tài khoản
        if (username == null) return false;
        username = username.trim();
        return username.length() >= 6 && !username.contains(" ") && username.matches("^[a-zA-Z0-9_]+$");
    }
    public static boolean isValidPassword(String password) {//kiểm tra format mật khẩu
        if (password == null) return false;
        password = password.trim();
        return password.length() >= 6 && !password.contains(" ") &&
               password.matches(".*[A-Z].*") &&
               password.matches(".*[a-z].*") &&
               password.matches(".*\\d.*") &&
               password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }
    public static boolean isValidEmail(String email) {//kiểm tra format email
        if (email == null) return false;
        email = email.trim();
        return !email.contains(" ") &&
               email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
    public static boolean isValidQuestion(String question) {//kiểm tra format câu hỏi xác nhận danh tính
        if (question == null) return false;
        return question.trim().length() >= 5;
    }
    public static boolean isValidAnswer(String answer) { //kiểm tra format câu trả lời xác nhận danh tính
        if (answer == null) return false;
        return answer.trim().length() >= 2;
    }
}