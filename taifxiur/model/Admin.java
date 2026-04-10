package model;

import controller.AuctionService;
import controller.WalletService;

import java.util.List;

public class Admin extends User { //quản trị viên
    public Admin(String username, String password, String email, String q1, String a1, String q2, String a2) {
        super(username, password, email, Role.ADMIN, q1, a1, q2, a2);
    }
    public void ban(User user) { //chặn người dùng
        if (user != null) {
            user.banUser();
        }
    }
    public void unban(User user) { //bỏ chặn người dùng
        if (user != null) {
            user.unbanUser();
        }
    }
    public List<Auction> viewAllAuctions(AuctionService service, AuctionStatus status) {
        if (service == null || status == null){
            throw new IllegalArgumentException();
        }
        return service.getAuctionsByStatus(status);
    }
    public void approveDeposit(WalletService service, String requestId) {
        service.approveDeposit(requestId, this);
    }
    public void rejectDeposit(WalletService service, String requestId) {
        service.rejectDeposit(requestId, this);
    }
    public void approveWithdraw(WalletService service, String requestId){
        service.approveWithdraw(requestId, this);
    }
    public void rejectWithdraw(WalletService service, String requestId){
        service.rejectWithdraw(requestId, this);
    }
}