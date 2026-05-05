package server.service;

public class UserLockedException extends RuntimeException {
    private final long secondsRemaining;

    public UserLockedException(long secondsRemaining) {
        super("Account is temporarily locked. Please try again in " + secondsRemaining + " seconds.");
        this.secondsRemaining = secondsRemaining;
    }

    public long getSecondsRemaining() {
        return secondsRemaining;
    }
}
