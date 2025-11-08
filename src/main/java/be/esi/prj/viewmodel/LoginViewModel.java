package be.esi.prj.viewmodel;

import be.esi.prj.model.services.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class LoginViewModel {

    private final UserService userService = UserService.getInstance();
    private final Logger logger = LoggerFactory.getLogger(LoginViewModel.class);

    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty message = new SimpleStringProperty();

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;

    @FXML
    public void initialize() {
        usernameField.textProperty().bindBidirectional(username);
        passwordField.textProperty().bindBidirectional(password);
        messageLabel.textProperty().bind(message);
        messageLabel.visibleProperty().bind(message.isNotEmpty());

        loginButton.setOnAction(event -> login());
        registerButton.setOnAction(this::openRegisterPage);

        Consumer<KeyEvent> enterKeyHandler = event -> {
            if (event.getCode() == KeyCode.ENTER) {
                login();
            }
        };
        usernameField.setOnKeyPressed(enterKeyHandler::accept);
        passwordField.setOnKeyPressed(enterKeyHandler::accept);
    }

    private void login() {
        try {
            if (!validateCredentials()){
                return;
            }
            if (authenticateUser()) {
                navigateToDashboard();
            }
        } catch (RuntimeException e) {
            message.set(e.getMessage());
        } catch (IOException e) {
            message.set("cant open dashboard");
            throw new RuntimeException(e);
        }
    }

    private void openRegisterPage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/be/esi/prj/fxml/register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("Registration");
            stage.show();
        } catch (IOException e) {
            logger.error("Error loading register page", e);
            message.set("Failed to load registration page");
        }
    }

    private boolean validateCredentials() {
        if (username.get() == null || username.get().isEmpty()) {
            message.set("Please enter a username.");
            return false;
        }

        if (password.get() == null || password.get().isEmpty()) {
            message.set("Please enter your password.");
            return false;
        }
        return true;
    }

    private boolean authenticateUser() {
        boolean authenticated = userService.authenticateUser(username.get(), password.get());
        if (authenticated) {
            message.set("Welcome, " + username.get() + "!");
        } else {
            message.set("Invalid credentials");
        }
        return authenticated;
    }

    private void navigateToDashboard() throws IOException {
        message.set("Connected");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/be/esi/prj/fxml/dashboard.fxml"));
        Parent root = loader.load();
        DashboardViewModel dashboardViewModel = loader.getController();

        Stage primaryStage = (Stage) loginButton.getScene().getWindow();
        dashboardViewModel.setPrimaryStage(primaryStage);

        Scene scene = createFullScreenScene(root);
        primaryStage.setTitle("Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Scene createFullScreenScene(Parent root) {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        return new Scene(root, bounds.getWidth(), bounds.getHeight());
    }
}
