import java.util.*;
import java.time.*;

abstract class User{
    private static int cnt = 0;
    private String username;
    private String password;
    private Role role;
    private String email;
    private boolean banned;
    private boolean active;
    private String question1;
    private String question2;
    private String answer1;
    private String answer2;
    private Instant lastTimeChangePass;

    public User(String username, String password, Role role, String email, String question1, String question2, String answer1, String answer2){
        cnt++;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.banned = false;
        this.active = true;
        this.question1 = question1.toLowerCase();
        this.question2 = question2.toLowerCase();
        this.answer1 = answer1.toLowerCase();
        this.answer2 = answer2.toLowerCase();
        this.lastTimeChangePass = Instant.now();
    }
    public String getUsername(){
        return username;
    }
    public String getPassword(){
        return password;
    }
    public Role getRole(){
        return role;
    }
    public String getEmail(){
        return email;
    }
    public boolean isBanned(){
        return banned;
    }
    public boolean isActive(){
        return active;
    }
    public boolean setPassWord(String oldPassWord, String password){
        if (hash(oldPassWord).equals(this.password)){
            this.password = hash(password);
            lastTimeChangePass = Instant.now();
            return true;
        }
        return false;
    }
    public boolean setBan(){
        if (!banned){
            banned = true;
            return true;
        }
        return false;
    }
    public boolean unBan(){
        if (banned){
            banned = false;
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
    public void Activate(){
        active = true;
    }
    public void deActivate(){
        active = false;
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
    public boolean verified(String answer1, String answer2){
        if (answer1 != null && !answer1.isEmpty() && answer2 != null && !answer2.isEmpty() && this.answer1.equals(answer1.toLowerCase()) && this.answer2.equals(answer2.toLowerCase())){
            return true;
        }
        return false;
    }
    public boolean forgotPassWord(String password, String answer1, String answer2){
        if (verified(answer1, answer2) && password != null && !password.isEmpty()){
            this.password = hash(password);
            lastTimeChangePass = Instant.now();
            return true;
        }
        return false;
    }
    public static int getCnt() {
        return User.cnt;
    }
    public boolean forgotUsername(String username, String email){
        if (email.equals(this.email)){
            this.username = username;
            return true;
        }
        return false;
    }
}

class UserService{
    private Map<String, User> userDB = new HashMap<>();
    public boolean register(String username, String password, Role role, String email, String question1, String question2, String answer1, String answer2) {
        if (username == null || password == null || email == null){
            return false;
        }
        username = username.trim();
        password = password.trim();
        email = email.trim();
        if (!Validator.validUsername(username)) {
            return false;
        }
        if (!Validator.validPassword(password)) {
            return false;
        }
        if (!Validator.validEmail(email)) {
            return false;
        }
        if (userDB.containsKey(username)) {
            return false;
        }
        password = hash(password);
        User user = null;
        if (role == Role.BIDDER) {
            user = new Bidder(username, password, role, email, question1, question2, answer1, answer2);
        }
        else if (role == Role.SELLER) {
            user = new Seller(username, password, role, email, question1, question2, answer1, answer2);
        }
        else if (role == Role.ADMIN) {
            user = new Admin(username, password, role, email, question1, question2, answer1, answer2);
        }
        else{
            return false;
        }
        userDB.put(username, user);
        return true;
    }
    public User login(String username, String password){
        if (username == null || password == null){
            return null;
        }
        username = username.trim();
        password = password.trim();
        User user = userDB.get(username);
        if (user == null || !(user.getPassword().equals(hash(password))) || !user.isBanned()){
            return null;
        }
        return user;
    }
}

class Validator{
    public static boolean validUsername(String username) {
        if (username == null){
            return false;
        }
        username = username.trim();
        if (username.matches(".*\\s.*") || !username.matches("[a-zA-Z0-9_]+") || username.length() < 3 || username.length() > 15){
            return false;
        }
        return true;
    }
    public static boolean validPassword(String password){
        if (password == null){
            return false;
        }
        password = password.trim();
        if (password.matches(".*\\s.*") || password.length() < 6){
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
    public Bidder(String username, String password, Role role, String email, String question1, String question2, String answer1, String answer2){
        super(username, password, role, email, question1, question2, answer1, answer2);
    }
}

class Seller extends User{
    private int totalTrade;
    public Seller(String username, String password, Role role, String email, String question1, String question2, String answer1, String answer2){
        super(username, password, role, email, question1, question2, answer1, answer2);
        totalTrade = 0;
    }
}

class Admin extends User{
    public Admin(String username, String password, Role role, String email, String question1, String question2, String answer1, String answer2){
        super(username, password, role, email, question1, question2, answer1, answer2);
    }
    public boolean setBan(User user) {
        if (user instanceof Admin){
            return false;
        }
        else{
            user.setBan();
            return true;
        }
    }
    public boolean unBan(User user){
        if (user instanceof Admin){
            return false;
        }
        user.unBan();
        return true;
    }
}

public class Main{
    public static void main(String[] args) {

    }
}
