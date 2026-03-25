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
        this.question1 = question1.toLowerCase();
        this.question2 = question2.toLowerCase();
        this.answer1 = answer1.toLowerCase();
        this.answer2 = answer2.toLowerCase();
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
    public boolean setUserName(String username){
        if (username != null && !username.isEmpty()){
            this.username = username;
            return true;
        }
        return false;
    }
    public boolean setPassWord(String oldPassWord, String password){
        if (oldPassWord.equals(this.password)){
            this.password = password;
            lastTimeChangePass = Instant.now();
            return true;
        }
        return false;
    }
    public boolean setEmail(String email){
        if (email != null && !email.isEmpty()){
            this.email = email;
            return true;
        }
        return false;
    }
    public void setStatus(){
        status = !status;
    }
    public void setActive(){
        active = !active;
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
    public boolean setQuestion1(String question1){
        if (question1 != null && !question1.isEmpty()){
            this.question1 = question1.toLowerCase();
            return true;
        }
        return false;
    }
    public boolean setQuestion2(String question2){
        if (question2 != null && !question2.isEmpty()){
            this.question2 = question2.toLowerCase();
            return true;
        }
        return false;
    }
    public boolean setAnswer1(String answer1){
        if (answer1 != null && !answer1.isEmpty()){
            this.answer1 = answer1.toLowerCase();
            return true;
        }
        return false;
    }
    public boolean setAnswer2(String answer2){
        if (answer2 != null && !answer2.isEmpty()){
            this.answer2 = answer2.toLowerCase();
            return true;
        }
        return false;
    }
    public boolean verifyer(String answer1, String answer2){
        if (answer1 != null && !answer1.isEmpty() && answer2 != null && !answer2.isEmpty() && this.answer1.equals(answer1.toLowerCase()) && this.answer2.equals(answer2.toLowerCase())){
            return true;
        }
        return false;
    }
    public boolean forgotPassWord(String password, String answer1, String answer2){
        if (verifyer(answer1, answer2) && password != null && !password.isEmpty()){
            this.password = password;
            lastTimeChangePass = Instant.now();
            return true;
        }
        return false;
    }
}

class UserService{
    private Map<String, User> userDB = new HashMap<>();
    public boolean register(String username, String password, String role, String email) {
        if (username == null || password == null || email == null){
            return false;
        }
        username = username.trim();
        password = password.trim();
        email = email.trim();
        if (!Validator.validUserName(username)) {
            return false;
        }
        if (!Validator.validPassWord(password)) {
            return false;
        }
        if (!Validator.validEmail(email)) {
            return false;
        }
        if (userDB.containsKey(username)) {
            return false;
        }
        if (role.equals("BIDDER")) {
            User user = new Bidder(username, password, role, email);
            userDB.put(username, user);
        } 
        else if (role.equals("SELLER")) {
            User user = new Seller(username, password, role, email);
            userDB.put(username, user);
        } 
        else if (role.equals("ADMIN")) {
            User user = new Admin(username, password, role, email);
            userDB.put(username, user);
        }
        return true;
    }
    public User login(String username, String password){
        if (username == null || password == null){
            return false;
        }
        username = username.trim();
        password = password.trim();
        User user = userDB.get(username);
        if (user == null || !user.getPassword().equals(password) || !user.isActive()){
            return null;
        }
        return user;
    }
}

class Validator{
    public static boolean validUserName(String username) {
        if (username == null){
            return false;
        }
        username = username.trim();
        if (username.isEmpty() || username.matches(".*\\s.*") || !username.matches("[a-zA-Z0-9_]+") || username.length() < 3 || username.length() > 15){
            return false;
        }
        return true;
    }
    public static boolean validPassWord(String password){
        if (password == null){
            return false;
        }
        password = password.trim();
        if (password.isEmpty() || password.matches(".*\\s.*") || password.length() < 6){
            return false;
        }
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");
        return hasDigit && hasLower && hasUpper && hasSpecial;
    }
    public static boolean validEmail(String email){
        if (email == null){
            return false;
        }
        email = email.trim();
        if (email.isEmpty() || email.matches(".*\\s.*")){
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
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
