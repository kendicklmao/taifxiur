package client.controller;

import javafx.embed.swing.JFXPanel;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import client.FakeAlertService;
import client.FakeAppContext;
import client.FakeNavigator;
import shared.network.Response;

import static org.junit.jupiter.api.Assertions.*;

public class LoginControllerTest {

    static {
        new JFXPanel();
    }

    private FakeAppContext fakeAppContext;
    private FakeNavigator fakeNavigator;
    private FakeAlertService fakeAlertService;
    private LoginController loginController;

    @BeforeEach
    void setUp() {
        fakeAppContext = new FakeAppContext();
        fakeNavigator = new FakeNavigator();
        fakeAlertService = new FakeAlertService();
        loginController = new LoginController(fakeAppContext, fakeNavigator, fakeAlertService);

        loginController.usernameField = new TextField();
        loginController.passwordField = new PasswordField();
    }

    @Test
    void handleLogin_whenSuccessAsBidder_navigatesToBidderHome() {
        loginController.usernameField.setText("bidder");
        loginController.passwordField.setText("password");
        Response successResponse = new Response("SUCCESS", "BIDDER,bidder");
        fakeAppContext.setResponseToReturn(successResponse);
        loginController.handleLogin();

        assertTrue(fakeAppContext.isConnectCalled());
        assertNotNull(fakeAppContext.getCurrentUser());
        assertEquals("bidder", fakeAppContext.getCurrentUser().getUsername());
        assertEquals("bidder_home.fxml", fakeNavigator.getLastSwitchedScene());
    }

    @Test
    void handleLogin_whenFailed_doesNotNavigate() {
        loginController.usernameField.setText("user");
        loginController.passwordField.setText("wrongpassword");
        Response errorResponse = new Response("ERROR", "Invalid credentials");
        fakeAppContext.setResponseToReturn(errorResponse);
        loginController.handleLogin();

        assertTrue(fakeAppContext.isConnectCalled());
        assertNull(fakeAppContext.getCurrentUser());
        assertTrue(fakeNavigator.getSwitchedScenes().isEmpty());
        assertEquals(1, fakeAlertService.getCallCount());
        assertEquals("Invalid credentials", fakeAlertService.getLastMessage());
    }

    @Test
    void handleLogin_whenConnectionFails_doesNotNavigate() {
        loginController.usernameField.setText("user");
        loginController.passwordField.setText("password");
        fakeAppContext.setExceptionToThrow(new RuntimeException("Connection failed"));
        loginController.handleLogin();
        assertTrue(fakeAppContext.isConnectCalled());
        assertNull(fakeAppContext.getCurrentUser());
        assertTrue(fakeNavigator.getSwitchedScenes().isEmpty());
        assertEquals(1, fakeAlertService.getCallCount());
        assertEquals("Cannot connect to Server!", fakeAlertService.getLastMessage());
    }

    @Test
    void goToRegister_callsNavigator() {
        loginController.goToRegister();
        assertEquals("register.fxml", fakeNavigator.getLastSwitchedScene());
    }

    @Test
    void goToForgotPassword_callsNavigator() {
        loginController.goToForgotPassword();
        assertEquals("forgot_password.fxml", fakeNavigator.getLastSwitchedScene());
    }
}