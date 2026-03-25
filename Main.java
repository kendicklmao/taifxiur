import java.util.*;
import java.time.*;
abstract class User{
    private static long cnt = 1;
    private String id;
    private String username;
    private String password;
    private String role;
    private String email;
    private boolean status;
    private boolean active;
    private String question1;
    private String question2;
    private String answer1;
    private String answer2;
    private Instant lastTimeChangePass;
    public User(String username, String password, String role, String email, String question1, String question2, String answer1, String answer2){
        this.id = String.valueOf(cnt ++);
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.status = true;
        this.active = true;
        this.question1 = question1;
        this.question2 = question2;
        this.answer1 = answer1;
        this.answer2 = answer2;
        this.lastTimeChangePass = Instant.now();
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
            lastTimeChangePass = Instant.now();
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
    public Instant getLastTimeChangePass(){
        return lastTimeChangePass;
    }
    public String getQuestion1(){
        return question1;
    }
    public String getQuestion2(){
        return question2;
    }
    public void setQuestion1(String question1){
        this.question1 = question1;
    }
    public void setQuestion2(String question2){
        this.question2 = question2;
    }
    public boolean verifyer(String answer1, String answer2){
        if (this.answer1.equals(answer1) && this.answer2.equals(answer2)){
            return true;
        }
        return false;
    }
    public void forgotPassWord(String password){
        this.password = password;
        lastTimeChangePass = Instant.now();
    }
}

class UserService{
    private Map<String, User> userDB = new HashMap<>();
    public boolean register(String id, String username, String password, String role, String email) {
        if (userDB.containsKey(username)) {
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
        return true;
    }
    public User login(String username, String password){
        if (!userDB.containsKey(username)) {
            return null;
        }
        User user = userDB.get(username);
        if (!user.getPassWord().equals(password)) {
            return null;
        }
        if (user.getStatus() == false) {
            return null;
        }
        return user;
    }
}

class Bidder extends User{
    public Bidder(String id, String username, String password, String role, String email){
        super(username, password, role, email, username, password, role, email);
    }
}

class Seller extends User{
    private long totalTrade;
    public Seller(String id, String username, String password, String role, String email){
        super(username, password, role, email, username, password, role, email);
        totalTrade = 0;
    }
}

class Admin extends User{
    public Admin(String id, String username, String password, String role, String email){
        super(username, password, role, email, username, password, role, email);
    }
    public void ban_unban(User u){
        u.setStatus();
    }
}

interface Sellable{
    
}

interface Biddable{

}

class BidTransaction {
    private Bidder bidder;
    private double amount;
    private Instant time;
    public BidTransaction(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.time = Instant.now();
    }
    public Bidder getBidder() {
        return bidder;
    }
    public double getAmount() {
        return amount;
    }
    public Instant getTime() {
        return time;
    }
}

class Auction{

}

abstract class Item{
    private String id;
    private String name;
    private String description;
    private double basePrice;
    private double currentPrice;
    private String sellerName;
    private static long cnt = 1;
    public Item(String name, String description, double basePrice, double currentPrice, String sellerName){
        this.id = String.valueOf(cnt++);
        this.description = description;
        this.basePrice = basePrice;
        this.currentPrice = currentPrice;
        this.sellerName = sellerName;
    }
}

public class Main{
    public static void main(String[] args) {
    }
}
