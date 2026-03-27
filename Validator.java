public class Validator {

    public static String normalizeUsername(String username) {
        if (username == null) return null;
        return username.trim().toLowerCase();
    }

    public static String normalizePassword(String password) {
        if (password == null) return null;
        return password.trim();
    }

    public static String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }

    public static String normalizeQuestion(String question) {
        if (question == null) return null;
        return question.trim();
    }

    public static String normalizeAnswer(String answer) {
        if (answer == null) return null;
        return answer.trim().toLowerCase();
    }

    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        username = username.trim();
        return username.length() >= 6 &&
               !username.contains(" ") &&
               username.matches("^[a-zA-Z0-9_]+$");
    }

    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        password = password.trim();
        return password.length() >= 6 &&
               !password.contains(" ") &&
               password.matches(".*[A-Z].*") &&
               password.matches(".*[a-z].*") &&
               password.matches(".*\\d.*") &&
               password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        email = email.trim();
        return !email.contains(" ") &&
               email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    public static boolean isValidQuestion(String question) {
        if (question == null) return false;
        return question.trim().length() >= 5;
    }

    public static boolean isValidAnswer(String answer) {
        if (answer == null) return false;
        return answer.trim().length() >= 2;
    }
}