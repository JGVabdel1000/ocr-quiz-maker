package be.esi.prj.viewmodel;

import be.esi.prj.model.orm.Folder;
import be.esi.prj.model.orm.User;
import be.esi.prj.model.services.FolderService;
import be.esi.prj.model.services.UserService;

import be.esi.prj.App;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class DashboardViewModel {

    private final Logger logger = LoggerFactory.getLogger(DashboardViewModel.class);

    private final UserService userService = UserService.getInstance();
    private final FolderService folderService = new FolderService();
    private final User currentUser = userService.getCurrentUser();

    private final StringProperty username = new SimpleStringProperty();
    private Stage primaryStage;

    @FXML private Menu userMenu;
    @FXML private FlowPane flowPane;
    @FXML private VBox sidebarInfoBox;

    /**
     * Initializes the dashboard view by setting up user menu and folder creation actions.
     */
    public void initialize() {
        username.set(userService.getCurrentUsername());
        userMenu.textProperty().bind(username);
        loadFolders();
    }

    /**
     * Handles user logout and navigates to the login page.
     */
    @FXML
    private void handleLogout() {
        try {
            userService.logout();
            navigateToLogin();
        } catch (Exception e) {
            logger.error("Logout failed", e);
        }
    }

    @FXML
    private void handleUploadImages(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/be/esi/prj/fxml/upload.fxml"));
            Parent root = loader.load();
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Stage uploadStage = new Stage();
            uploadStage.setScene(new Scene(root));
            uploadStage.setTitle("Upload image(s)");

            uploadStage.initOwner(currentStage);
            uploadStage.initModality(Modality.APPLICATION_MODAL);

            uploadStage.show();
        } catch (IOException e) {
            logger.error("Failed to load upload images view", e);
        }
    }

    /**
     * Creates a new folder and adds it to the UI.
     */
    @FXML
    private void createFolder() {
        String folderName = promptFolderName();
        Folder newFolder = folderService.createFolder(folderName, currentUser);
        if (newFolder != null) {
            addFolderToUI(newFolder);
            logger.info("Folder created: {}", folderName);
        } else {
            logger.error("Failed to create folder.");
        }
    }

    private void updateSidebarWithFolderStats(int folderID) {
        sidebarInfoBox.getChildren().clear();

        int total = folderService.getTotalQuestionsCount(folderID);
        int again = folderService.getAgainQuestionsCount(folderID);
        int hard = folderService.getHardQuestionsCount(folderID);
        int good = folderService.getGoodQuestionsCount(folderID);
        int easy = folderService.getEasyQuestionsCount(folderID);

        Label totalLabel = new Label("Total : " + total);
        totalLabel.getStyleClass().add("total-cards-label");

        Label hardLabel = new Label("Hard : " + hard);
        hardLabel.getStyleClass().addAll("hard-label", "sidebar-labels");

        Label goodLabel = new Label("Good : " + good);
        goodLabel.getStyleClass().addAll("good-label", "sidebar-labels");

        Label easyLabel = new Label("Easy : " + easy);
        easyLabel.getStyleClass().addAll("easy-label", "sidebar-labels");

        Label againLabel = new Label("Again : " + again);
        againLabel.getStyleClass().addAll("again-label", "sidebar-labels");

        sidebarInfoBox.getChildren().addAll(totalLabel, againLabel, hardLabel, goodLabel, easyLabel);
    }



    /**
     * Loads all folders associated with the current user and adds them to the UI.
     */
    private void loadFolders() {
        List<Folder> folders = folderService.getFoldersForUser(currentUser);
        for (Folder folder : folders) {
            addFolderToUI(folder);
        }
    }

    /**
     * Creates a VBox containing the folder icon and name, and sets up a context menu for renaming and deleting.
     */
    private VBox createFolderBox(Folder folder) {
        ImageView folderIcon = createFolderIcon();
        Label folderNameLabel = createFolderLabel(folder);
        ContextMenu contextMenu = createFolderContextMenu(folder, folderNameLabel);

        VBox folderBox = new VBox(folderIcon, folderNameLabel);
        folderBox.setAlignment(javafx.geometry.Pos.CENTER);
        folderBox.getStyleClass().add("folderContainer");

        folderBox.setOnContextMenuRequested(event -> {
                contextMenu.show(folderBox, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        );

        folderBox.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                updateSidebarWithFolderStats(folder.getFolderId());
            }
            if (event.getClickCount() == 2) {
                loadQuestions(folder.getFolderId());
            }
        });

        return folderBox;
    }

    @FXML
    private void loadQuestions(int folderId) {
        try {
            Stage parentStage = (Stage) flowPane.getScene().getWindow();
            Parent parentRoot = parentStage.getScene().getRoot();
            applyBlurEffect(parentRoot, true);

            FXMLLoader loader = new FXMLLoader(App.class.getResource("/be/esi/prj/fxml/card.fxml"));
            Parent root = loader.load();

            QuizSessionViewModel controller = loader.getController();
            controller.initializeWithFolder(folderId);

            Stage stage = new Stage();
            stage.setTitle("Quiz Session");
            stage.setScene(new Scene(root, 500, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(parentStage);
            stage.centerOnScreen();
            stage.setOnHidden(e -> {
                applyBlurEffect(parentRoot, false);
                updateSidebarWithFolderStats(folderId);
            });
            stage.show();
        } catch (IOException e) {
            logger.error("Failed to load quiz session view", e);
        }
    }

    private void applyBlurEffect(Parent root, boolean apply) {
        if (apply) {
            GaussianBlur blur = new GaussianBlur(8);
            root.setEffect(blur);
        } else {
            root.setEffect(null);
        }
    }


    /**
     * Creates and returns a folder icon ImageView.
     */
    private ImageView createFolderIcon() {
        ImageView folderIcon = new ImageView(new javafx.scene.image.Image(
                getClass().getResource("/be/esi/prj/images/folder-icon.png").toExternalForm()));
        folderIcon.setPreserveRatio(true);
        folderIcon.setPickOnBounds(true);
        folderIcon.setCursor(javafx.scene.Cursor.HAND);
        folderIcon.setFitWidth(80);
        return folderIcon;
    }

    /**
     * Creates and returns a label displaying the folder's name.
     */
    private Label createFolderLabel(Folder folder) {
        Label folderNameLabel = new Label(folder.getName());
        folderNameLabel.getStyleClass().add("folderName");
        return folderNameLabel;
    }

    /**
     * Creates and returns a context menu with rename and delete options for a folder.
     */
    private ContextMenu createFolderContextMenu(Folder folder, Label folderNameLabel) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem renameItem = createRenameMenuItem(folder, folderNameLabel);
        MenuItem deleteItem = createDeleteMenuItem(folder, folderNameLabel);

        contextMenu.getItems().addAll(renameItem, deleteItem);
        contextMenu.getStyleClass().add("contextMenu");
        return contextMenu;
    }

    private MenuItem createRenameMenuItem(Folder folder, Label folderNameLabel) {
        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(folderNameLabel.getText());
            dialog.setTitle("Rename Folder");
            dialog.setHeaderText("Enter a new folder name:");
            dialog.setContentText("Name:");
            dialog.showAndWait().ifPresent(newName -> {
                if (!newName.trim().isEmpty()) {
                    folder.setName(newName);
                    folderNameLabel.setText(newName.trim());
                    folderService.updateFolder(folder);
                }
            });
        });
        return renameItem;
    }

    private MenuItem createDeleteMenuItem(Folder folder, Label folderNameLabel) {
        MenuItem deleteItem = new MenuItem("delete");
        deleteItem.setOnAction(e -> deleteFolder(folder, folderNameLabel));
        return deleteItem;
    }

    /**
     * Deletes the specified folder and removes it from the UI.
     */
    private void deleteFolder(Folder folder, Label folderNameLabel) {
        folderService.deleteFolder(folder.getFolderId());
        flowPane.getChildren().remove(folderNameLabel.getParent());
        logger.info("Folder deleted: {}", folderNameLabel.getText());
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    private String promptFolderName() {
        String defaultName = "new folder";
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New folder");
        dialog.setContentText("Folder Name:");
        dialog.showAndWait();

        return dialog.getResult().trim().isEmpty() ? defaultName : dialog.getResult().trim();
    }

    private void addFolderToUI(Folder folder) {
        VBox folderBox = createFolderBox(folder);
        flowPane.getChildren().add(folderBox);
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/be/esi/prj/fxml/login.fxml"));
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle("Login");
            primaryStage.show();
        } catch (IOException e) {
            logger.error("Error loading login page", e);
        }
    }
}