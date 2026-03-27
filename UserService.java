import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lockUntil = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_LOCK_SECONDS = 5;

    public boolean register(String username, String password, String email,
                            String q1, String a1, String q2, String a2,
                            Role role) {

        username = Validator.normalizeUsername(username);
        password = Validator.normalizePassword(password);
        email = Validator.normalizeEmail(email);
        q1 = Validator.normalizeQuestion(q1);
        q2 = Validator.normalizeQuestion(q2);
        a1 = Validator.normalizeAnswer(a1);
        a2 = Validator.normalizeAnswer(a2);

        if (!Validator.isValidUsername(username) ||
            !Validator.isValidPassword(password) ||
            !Validator.isValidEmail(email) ||
            !Validator.isValidQuestion(q1) ||
            !Validator.isValidQuestion(q2) ||
            !Validator.isValidAnswer(a1) ||
            !Validator.isValidAnswer(a2)) {
            return false;
        }

        User user;

        if (role == Role.BIDDER) {
            user = new Bidder(username, password, email, q1, a1, q2, a2);
        } else if (role == Role.SELLER) {
            user = new Seller(username, password, email, q1, a1, q2, a2);
        } else if (role == Role.ADMIN) {
            user = new Admin(username, password, email, q1, a1, q2, a2);
        } else {
            return false;
        }

        return users.putIfAbsent(username, user) == null;
    }

    public User login(String username, String password) {
        if (username == null || password == null) return null;

        username = Validator.normalizeUsername(username);
        password = Validator.normalizePassword(password);

        Instant now = Instant.now();

        if (lockUntil.containsKey(username)) {
            Instant unlockTime = lockUntil.get(username);
            if (now.isBefore(unlockTime)) {
                return null;
            } else {
                lockUntil.remove(username);
            }
        }

        User user = users.get(username);

        if (user == null) return null;
        if (user.isBanned()) return null;

        if (user.getPassword().equals(password)) {
            failedAttempts.remove(username);
            lockUntil.remove(username);
            return user;
        } else {
            int attempts = failedAttempts.getOrDefault(username, 0) + 1;
            failedAttempts.put(username, attempts);

            if (attempts >= MAX_ATTEMPTS) {
                long lockSeconds = BASE_LOCK_SECONDS * attempts;
                lockUntil.put(username, now.plusSeconds(lockSeconds));
            }
            return null;
        }
    }

    public boolean exists(String username) {
        if (username == null) return false;
        return users.containsKey(Validator.normalizeUsername(username));
    }
}