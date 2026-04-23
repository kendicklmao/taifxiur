package shared.models;

import java.time.Instant;

public class AdminActionLog {
    private int id;
    private String adminUsername;
    private String targetUsername;
    private String action;
    private Instant actionTime;

    public AdminActionLog(int id, String adminUsername, String targetUsername, String action, Instant actionTime) {
        this.id = id;
        this.adminUsername = adminUsername;
        this.targetUsername = targetUsername;
        this.action = action;
        this.actionTime = actionTime;
    }

    public int getId() {
        return id;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public String getAction() {
        return action;
    }

    public Instant getActionTime() {
        return actionTime;
    }

    @Override
    public String toString() {
        return String.format("Admin '%s' %s user '%s' at %s",
                adminUsername, action.toLowerCase(), targetUsername, actionTime.toString());
    }
}

