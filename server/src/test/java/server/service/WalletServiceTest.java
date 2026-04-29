package server.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import server.database.DatabaseConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class WalletServiceTest {

    private WalletService walletService;
    private UserService userService;

    @Before
    public void setUp() {
        walletService = new WalletService();
        userService = new UserService();
        userService.initializeDefaultUsers();
    }

    @After
    public void tearDown() {
        DatabaseConfig.closeDataSource();
    }

    @Test
    public void testCreateDepositRequest() {
        String result = walletService.createDepositRequest("bidder", new BigDecimal("100.00"));
        assertNull(result);

        List<Map<String, String>> requests = walletService.getPendingDepositRequests();
        assertFalse(requests.isEmpty());
    }

    @Test
    public void testApproveDepositRequest() {
        walletService.createDepositRequest("bidder", new BigDecimal("100.00"));
        List<Map<String, String>> requests = walletService.getPendingDepositRequests();
        String requestId = requests.get(0).get("id");

        String result = walletService.approveDeposit(requestId, "admin");
        assertNull(result);

        BigDecimal balance = walletService.getWalletBalance("bidder");
        assertEquals(0, balance.compareTo(new BigDecimal("100.00")));
    }

    @Test
    public void testRejectDepositRequest() {
        walletService.createDepositRequest("bidder", new BigDecimal("100.00"));
        List<Map<String, String>> requests = walletService.getPendingDepositRequests();
        String requestId = requests.get(0).get("id");

        String result = walletService.rejectDeposit(requestId, "admin");
        assertNull(result);

        BigDecimal balance = walletService.getWalletBalance("bidder");
        assertEquals(0, balance.compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testCreateWithdrawRequest() {
        walletService.createDepositRequest("seller", new BigDecimal("200.00"));
        List<Map<String, String>> deposits = walletService.getPendingDepositRequests();
        walletService.approveDeposit(deposits.get(0).get("id"), "admin");

        String result = walletService.createWithdrawRequest("seller", new BigDecimal("50.00"), "Bank", "12345");
        assertNull(result);

        List<Map<String, String>> withdraws = walletService.getPendingWithdrawRequests();
        assertFalse(withdraws.isEmpty());
    }

    @Test
    public void testApproveWithdrawRequest() {
        walletService.createDepositRequest("seller", new BigDecimal("200.00"));
        List<Map<String, String>> deposits = walletService.getPendingDepositRequests();
        walletService.approveDeposit(deposits.get(0).get("id"), "admin");

        walletService.createWithdrawRequest("seller", new BigDecimal("50.00"), "Bank", "12345");
        List<Map<String, String>> withdraws = walletService.getPendingWithdrawRequests();
        String requestId = withdraws.get(0).get("id");

        String result = walletService.approveWithdraw(requestId, "admin");
        assertNull(result);

        BigDecimal balance = walletService.getWalletBalance("seller");
        assertEquals(0, balance.compareTo(new BigDecimal("150.00")));
    }

    @Test
    public void testRejectWithdrawRequest() {
        walletService.createDepositRequest("seller", new BigDecimal("200.00"));
        List<Map<String, String>> deposits = walletService.getPendingDepositRequests();
        walletService.approveDeposit(deposits.get(0).get("id"), "admin");

        walletService.createWithdrawRequest("seller", new BigDecimal("50.00"), "Bank", "12345");
        List<Map<String, String>> withdraws = walletService.getPendingWithdrawRequests();
        String requestId = withdraws.get(0).get("id");

        String result = walletService.rejectWithdraw(requestId, "admin");
        assertNull(result);

        BigDecimal balance = walletService.getWalletBalance("seller");
        assertEquals(0, balance.compareTo(new BigDecimal("200.00")));
    }
}

