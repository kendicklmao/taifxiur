package server.controller;

import server.service.AuctionService;
import server.service.StorageService;
import server.service.UserService;
import server.service.WalletService;

import java.util.HashMap;
import java.util.Map;

public class HandlerFactory {

    public static Map<String, RequestHandler> createHandlers(UserService userService, AuctionService auctionService,
            WalletService walletService, StorageService storageService) {

        Map<String, RequestHandler> handlers = new HashMap<>();

        handlers.put("LOGIN", new LoginHandler(userService));
        handlers.put("REGISTER", new RegisterHandler(userService));
        handlers.put("FORGOT_PASSWORD_INIT", new ForgotPasswordInitHandler(userService));
        handlers.put("RESET_PASSWORD", new ResetPasswordHandler(userService));
        handlers.put("CHANGE_PASSWORD", new ChangePasswordHandler(userService));
        handlers.put("GET_AUCTIONS", new GetAuctionsHandler(auctionService));
        handlers.put("GET_AUCTION_INFO", new GetAuctionInfoHandler(auctionService));
        handlers.put("PLACE_BID", new PlaceBidHandler(auctionService, walletService, userService));
        handlers.put("REGISTER_AUTOBID", new RegisterAutobidHandler(userService, auctionService));
        handlers.put("ITEM_PAID", new ItemPaidHandler(auctionService, userService));
        handlers.put("GET_FINISHED_AUCTIONS", new GetFinishedAuctionsHandler(auctionService));
        handlers.put("CREATE_AUCTION", new CreateAuctionHandler(auctionService, userService, storageService));
        handlers.put("GET_SELLER_AUCTIONS", new GetSellerAuctionsHandler(auctionService));
        handlers.put("TERMINATE_AUCTION", new TerminateAuctionHandler(auctionService));
        handlers.put("LOGOUT", new LogOutHandler(userService));
        handlers.put("GET_ALL_USERS", new GetAllUsersHandler(userService));
        handlers.put("BAN_USER", new BanUserHandler(userService));
        handlers.put("UNBAN_USER", new UnbanUserHandler(userService));
        handlers.put("GET_ADMIN_ACTION_LOGS", new GetAdminActionLogsHandler(userService));
        handlers.put("GET_PENDING_DEPOSIT_REQUESTS", new GetPendingDepositRequestsHandler(userService, walletService));
        handlers.put("GET_PENDING_WITHDRAW_REQUESTS", new GetPendingWithdrawRequestsHandler(userService, walletService));
        handlers.put("APPROVE_DEPOSIT_REQUEST", new ApproveDepositRequestHandler(walletService));
        handlers.put("REJECT_DEPOSIT_REQUEST", new RejectDepositRequestHandler(walletService));
        handlers.put("APPROVE_WITHDRAW_REQUEST", new ApproveWithdrawRequestHandler(walletService));
        handlers.put("REJECT_WITHDRAW_REQUEST", new RejectWithdrawRequestHandler(walletService));
        handlers.put("CREATE_DEPOSIT_REQUEST", new CreateDepositRequestHandler(walletService));
        handlers.put("CREATE_WITHDRAW_REQUEST", new CreateWithdrawRequestHandler(walletService));
        handlers.put("GET_WALLET_BALANCE", new GetWalletBalanceHandler(walletService));

        return handlers;
    }
}