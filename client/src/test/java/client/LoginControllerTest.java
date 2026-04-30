package client;

import javafx.embed.swing.JFXPanel;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.network.Response;

import static org.junit.jupiter.api.Assertions.*;

public class LoginControllerTest {

    // Static block to initialize JavaFX Toolkit
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

        // Initialize JavaFX fields
        loginController.usernameField = new TextField();
        loginController.passwordField = new PasswordField();
    }

    @Test
    void handleLogin_whenSuccessAsBidder_navigatesToBidderHome() {
        // Arrange
        loginController.usernameField.setText("bidder");
        loginController.passwordField.setText("password");
        Response successResponse = new Response("SUCCESS", "BIDDER,bidder");
        fakeAppContext.setResponseToReturn(successResponse);

        // Act
        loginController.handleLogin();

        // Assert
        assertTrue(fakeAppContext.isConnectCalled());
        assertNotNull(fakeAppContext.getCurrentUser());
        assertEquals("bidder", fakeAppContext.getCurrentUser().getUsername());
        assertEquals("bidder_home.fxml", fakeNavigator.getLastSwitchedScene());
    }

    @Test
    void handleLogin_whenFailed_doesNotNavigate() {
        // Arrange
        loginController.usernameField.setText("user");
        loginController.passwordField.setText("wrongpassword");
        Response errorResponse = new Response("ERROR", "Invalid credentials");
        fakeAppContext.setResponseToReturn(errorResponse);

        // Act
        loginController.handleLogin();

        // Assert
        assertTrue(fakeAppContext.isConnectCalled());
        assertNull(fakeAppContext.getCurrentUser());
        assertTrue(fakeNavigator.getSwitchedScenes().isEmpty());
        assertEquals(1, fakeAlertService.getCallCount());
        assertEquals("Invalid credentials", fakeAlertService.getLastMessage());
    }

    @Test
    void handleLogin_whenConnectionFails_doesNotNavigate() {
        // Arrange
        loginController.usernameField.setText("user");
        loginController.passwordField.setText("password");
        fakeAppContext.setExceptionToThrow(new RuntimeException("Connection failed"));

        // Act
        loginController.handleLogin();

        // Assert
        assertTrue(fakeAppContext.isConnectCalled());
        assertNull(fakeAppContext.getCurrentUser());
        assertTrue(fakeNavigator.getSwitchedScenes().isEmpty());
        assertEquals(1, fakeAlertService.getCallCount());
        assertEquals("Cannot connect to Server!", fakeAlertService.getLastMessage());
    }

    @Test
    void goToRegister_callsNavigator() {
        // Act
        loginController.goToRegister();

        // Assert
        assertEquals("register.fxml", fakeNavigator.getLastSwitchedScene());
    }

    @Test
    void goToForgotPassword_callsNavigator() {
        // Act
        loginController.goToForgotPassword();

        // Assert
        assertEquals("forgot_password.fxml", fakeNavigator.getLastSwitchedScene());
    }
}
