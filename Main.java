abstract class user{
    private String id;
    private String username;
    private String password;
    private String role;
    private String email;
    private boolean status;
    private boolean active;
    public user(String id, String username, String password, String role, String email, boolean active){
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.status = true;
        this.active = active;
    }
}

class bidder extends user{
    public bidder(String id, String username, String password, String role, String email, boolean active){
        super(id, username, password, role, email, active);
    }
}

class seller extends user{
    public seller(String id, String username, String password, String role, String email, boolean active){
        super(id, username, password, role, email, active);
    }
}

class admin extends user{
    public admin(String id, String username, String password, String role, String email, boolean active){
        super(id, username, password, role, email, active);
    }
}