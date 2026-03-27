public abstract class User {
    private String username;
    private String password;
    private String email;
    private boolean isBanned;
    private Role role;

    private String securityQuestion1;
    private String securityAnswer1;
    private String securityQuestion2;
    private String securityAnswer2;

    public User(String username, String password, String email, Role role,
            String q1, String a1, String q2, String a2) {

    this.username = username;
    this.password = password;
    this.email = email;
    this.role = role;
    this.isBanned = false;
    this.securityQuestion1 = q1;
    this.securityAnswer1 = a1;
    this.securityQuestion2 = q2;
    this.securityAnswer2 = a2;
}

    public String getUsername() { 
        return username; 
    }

    public String getPassword() { 
        return password; 
    }

    public String getEmail() { 
        return email; 
    }

    public boolean isBanned() { 
        return isBanned; 
    }

    public Role getRole() { 
        return role; 
    }

    public String getSecurityQuestion1() { 
        return securityQuestion1; 
    }

    public String getSecurityQuestion2() { 
        return securityQuestion2; 
    }

    public boolean setPassword(String newPassword) {
        newPassword = Validator.normalizePassword(newPassword);
        if (!Validator.isValidPassword(newPassword)) {
            return false;
        }
        this.password = newPassword;
        return true;
    }

    public boolean setEmail(String email) {
        email = Validator.normalizeEmail(email);
        if (!Validator.isValidEmail(email)) {
            return false;
        }
        this.email = email;
        return true;
    }

    public void banUser() { 
        this.isBanned = true; 
    }

    public void unbanUser() { 
        this.isBanned = false; 
    }

    public boolean verifySecurityAnswers(String a1, String a2) {
        a1 = Validator.normalizeAnswer(a1);
        a2 = Validator.normalizeAnswer(a2);
        return securityAnswer1.equals(a1) && securityAnswer2.equals(a2);
    }

    public boolean resetPassword(String a1, String a2, String newPassword) {
        if (verifySecurityAnswers(a1, a2)) {
            return setPassword(newPassword);
        }
        return false;
    }

    public boolean changePassword(String oldPassword, String newPassword) {
        oldPassword = Validator.normalizePassword(oldPassword);
        if (this.password.equals(oldPassword)) {
            return setPassword(newPassword);
        }
        return false;
    }

    public abstract void displayInfo();
}