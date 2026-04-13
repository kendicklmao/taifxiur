package server.service;

import shared.enums.Role;
import shared.models.Admin;
import shared.models.Bidder;
import shared.models.Seller;
import shared.models.User;
import shared.utils.Validator;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>(); //lưu tài khoản
    private final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap<>(); //lưu số lần đăng nhập thất bại của từng tài khoản
    private final ConcurrentHashMap<String, Instant> lockUntil = new ConcurrentHashMap<>(); //lưu số giây bị ban của từng tài khoản
    private static final int MAX_ATTEMPTS = 5; //số lượt đăng nhập thất bại tối đa
    private static final long BASE_LOCK_SECONDS = 2; //số giây cơ sở để vô hiệu hóa nếu đăng nhập thất bại

    //nghịch chim
    public UserService() {
        this.register("seller123", "Admin@123", "seller@gmail.com", "q", "a", "q", "a", Role.SELLER);
        this.register("bidder123", "Admin@123", "bidder@gmail.com", "q", "a", "q", "a", Role.BIDDER);
        this.register("admin123", "Admin@123", "admin@gmail.com", "q", "a", "q", "a", Role.ADMIN);
    }

    public boolean register(String username, String password, String email, String q1, String a1, String q2, String a2, Role role) {
        if (!Validator.isValidUsername(username)) {
            System.out.println("Username sai định dạng r ngu");
            return false;
        }
        if (!Validator.isValidPassword(password)) {
            System.out.println("Password sai định dạng r ngu");
            return false;
        }
        if (!Validator.isValidEmail(email)) {
            System.out.println("Email sai định dạng r ngu");
            return false;
        }

        username = Validator.normalizeUsername(username);
        password = Validator.normalizePassword(password);
        email = Validator.normalizeEmail(email);
        q1 = Validator.normalizeQuestion(q1);
        q2 = Validator.normalizeQuestion(q2);
        a1 = Validator.normalizeAnswer(a1);
        a2 = Validator.normalizeAnswer(a2);

        User user;
        if (role == Role.BIDDER) {
            user = new Bidder(username, password, email, q1, a1, q2, a2);
        } else if (role == Role.SELLER) {
            user = new Seller(username, password, email, q1, a1, q2, a2);
        } else if (role == Role.ADMIN) {
            user = new Admin(username, password, email, q1, a1, q2, a2);
        } else {
            System.out.println("Role không hợp lệ!");
            return false;
        }

        boolean success = (users.putIfAbsent(username, user) == null);
        if (!success) {
            System.out.println("Username này đã tồn tại trong Sổ!");
        } else {
            System.out.println("Đăng ký thành công vào sổ!");
        }
        return success;
    }

    public User login(String username, String password) { //đăng nhập tài khoản
        if (username == null || password == null) {
            return null;
        }
        username = Validator.normalizeUsername(username);
        password = Validator.normalizePassword(password);
        Instant now = Instant.now();
        if (lockUntil.containsKey(username)) {
            Instant unlockTime = lockUntil.get(username);
            if (now.isBefore(unlockTime)) {
                return null;
            } else {
                lockUntil.remove(username);
                failedAttempts.remove(username);
            }
        }
        User user = users.get(username);
        if (user == null) {
            return null;
        }
        if (user.isBanned()) {
            return null;
        }
        if (user.checkPassword(password)) {
            failedAttempts.remove(username);
            lockUntil.remove(username);
            return user;
        } else {
            int attempts = failedAttempts.getOrDefault(username, 0) + 1;
            failedAttempts.put(username, attempts);
            long lockSeconds;
            if (attempts >= MAX_ATTEMPTS) {
                if (attempts > 11) {
                    lockSeconds = 3600;
                } else {
                    lockSeconds = (long) Math.pow(BASE_LOCK_SECONDS, (attempts - 1));
                }
                lockUntil.put(username, now.plusSeconds(lockSeconds));
            }
            return null;
        }
    }

    public boolean exists(String username) {//kiểm tra xem đã tồn tại tài khoản chưa
        if (username == null) return false;
        return users.containsKey(Validator.normalizeUsername(username));
    }
    public User getUser(String username) {
        return users.get(username);
    }
}