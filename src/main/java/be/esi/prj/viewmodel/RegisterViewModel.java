package be.esi.prj.viewmodel;

import be.esi.prj.App;
import be.esi.prj.model.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * ViewModel for user registration screen.
 */
public class RegisterViewModel {

    private final UserService userService = UserService.getInstance();
    private final Logger logger = LoggerFactory.getLogger(RegisterViewModel.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button submitButton;
    @FXML private Button loginButton;


    @FXML
    public void initialize() {
        setupBindings();
        setupEventHandlers();

        Consumer<KeyEvent> enterKeyHandler = event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleRegistration();
            }
        };
        usernameField.setOnKeyPressed(enterKeyHandler::accept);
        passwordField.setOnKeyPressed(enterKeyHandler::accept);
        confirmPasswordField.setOnKeyPressed(enterKeyHandler::accept);
    }

    /**
     * Handles the user registration process.
     * Delegates registration logic to the ViewModel and navigates to the login view
     * upon success.
     */
    private void handleRegistration() {
        if (userService.registerUser(
                usernameField.getText(),
                passwordField.getText(),
                confirmPasswordField.getText()
        )) {
            clearPasswordFields();
            navigateToLogin();
        }
    }

    /**
     * Navigates to the login view.
     * Replaces the current scene with the login view scene.
     */
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/be/esi/prj/fxml/login.fxml"));

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        }catch (IOException e) {
            logger.error("Error loading login page", e);
            userService.messageProperty().set("Failed to load login page");
        }
    }

    private void setupBindings() {
        messageLabel.textProperty().bind(userService.messageProperty());
        messageLabel.visibleProperty().bind(userService.messageProperty().isNotEmpty());
    }

    private void setupEventHandlers() {
        submitButton.setOnAction(e -> handleRegistration());
        loginButton.setOnAction(e -> navigateToLogin());
    }

    private void clearPasswordFields() {
        passwordField.clear();
        confirmPasswordField.clear();
    }
}
