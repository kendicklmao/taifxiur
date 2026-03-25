import java.util.*;
abstract class User{
    private static long cnt = 1;
    private String id;
    private String username;
    private String password;
    private String role;
    private String email;
    private boolean status;
    private boolean active;
    public User(String id, String username, String password, String role, String email){
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.status = true;
        this.active = true;
    }
    public String getID(){
        return id;
    }
    public String getUserName(){
        return username;
    }
    public String getPassWord(){
        return password;
    }
    public String getRole(){
        return role;
    }
    public String getEmail(){
        return email;
    }
    public boolean getStatus(){
        return status;
    }
    public boolean getActive(){
        return active;
    }
    public void setUserName(String username){
        this.username = username;
    }
    public boolean setPassWord(String oldPassWord, String password){
        if (oldPassWord.equals(this.password)){
            this.password = password;
            return true;
        }
        else{
            return false;
        }
    }
    public void setEmail(String email){
        this.email = email;
    }
    public void setStatus(){
        if (status == true){
            status = false;
        }
        else{
            status = true;
        }
    }
    public void setActive(){
        if (active == true){
            active = false;
        }
        else{
            active = true;
        }
    }

    public void incrID(){
        cnt ++;
    }

    public static String getCNT(){
        return String.valueOf(cnt);
    }
}

class UserService{
    private Map<String, User> userDB = new HashMap<>();
    public boolean register(String id, String username, String password, String role, String email) {
        if (userDB.containsKey(username)) {
            System.out.println("Username not available");
            return false;
        }
        if (role.equals("BIDDER")) {
            User user = new Bidder(id, username, password, role, email);
            userDB.put(username, user);
        } 
        else if (role.equals("SELLER")) {
            User user = new Seller(id, username, password, role, email);
            userDB.put(username, user);
        } 
        else if (role.equals("ADMIN")) {
            User user = new Admin(id, username, password, role, email);
            userDB.put(username, user);
        }
        System.out.println("Success");
        return true;
    }
    public User login(String username, String password){
        if (!userDB.containsKey(username)) {
            System.out.println("Username incorrect");
            return null;
        }
        User user = userDB.get(username);
        if (!user.getPassWord().equals(password)) {
            System.out.println("Password incorrect");
            return null;
        }
        if (user.getStatus() == false) {
            System.out.println("Ban");
            return null;
        }
        System.out.println("Success");
        return user;
    }
}

class Bidder extends User{
    public Bidder(String id, String username, String password, String role, String email){
        super(id, username, password, role, email);
    }
}

class Seller extends User{
    public Seller(String id, String username, String password, String role, String email){
        super(id, username, password, role, email);
    }
}

class Admin extends User{
    public Admin(String id, String username, String password, String role, String email){
        super(id, username, password, role, email);
    }
}

public class Main{
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        UserService service = new UserService();
        System.out.print("Username: ");
        String username = sc.nextLine();
        System.out.print("Password: ");
        String password = sc.nextLine();
        System.out.print("Email: ");
        String email = sc.nextLine();
        System.out.print("Role (BIDDER/SELLER/ADMIN): ");
        String role = sc.nextLine();
        service.register(User.getCNT(), username, password, role, email);
    }
}
