package be.esi.prj.viewmodel;

import be.esi.prj.model.orm.Folder;
import be.esi.prj.model.services.*;
import be.esi.prj.utils.QuizResult;
import be.esi.prj.utils.QuizTask;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class QuizUploadViewModel {

    private final Logger logger = LoggerFactory.getLogger(QuizUploadViewModel.class);
    private final OcrService ocrService = new OcrService();
    private final FolderService folderService = new FolderService();
    private final UserService userService = UserService.getInstance();
    private final QuizService quizService = QuizService.getInstance();
    private List<Folder> availableFolders;

    private final Object quizResultsLock = new Object();


    @FXML private Button browseButton;
    @FXML private VBox vboxDropArea;
    @FXML private Label progressIndicator;

    public void initialize() {
        setUpDragAndDrop();
        vboxDropArea.setOnDragExited(this::handleDragExited);
        browseButton.setOnAction(e -> chooseFiles());
        availableFolders = folderService.getFoldersForUser(userService.getCurrentUser());
    }

    /**
     * Sets up the drag-and-drop functionality for the drop area.
     */
    private void setUpDragAndDrop() {
        // Handle drag over event to accept file transfer if files are dragged over the drop area
        vboxDropArea.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                vboxDropArea.getStyleClass().add("drag-over");
            }
            event.consume(); // Consume the event to prevent further propagation
        });
        // Handle the drop event to process files dropped in the drop area
        vboxDropArea.setOnDragDropped(this::handleFileDrop);
    }

    /**
     * Handles the file drop event by validating and processing the dropped files.
     */
    private void handleFileDrop(DragEvent event) {
        var db = event.getDragboard();
        if (db.hasFiles()) {
            progressIndicator.setVisible(true);
            List<File> droppedFiles = db.getFiles();
            List<File> validImageFiles = filterValidImageFiles(droppedFiles);
            processValidFiles(validImageFiles);  // Process only valid image files
        }
        event.setDropCompleted(true);
        vboxDropArea.getStyleClass().remove("drag-over");
        event.consume();  // Consume the event to prevent further propagation
    }

    private void handleDragExited(DragEvent event) {
        // Remove the drag-over style when the drag exits
        vboxDropArea.getStyleClass().remove("drag-over");
        event.consume();
    }


    /**
     * Filters out invalid files (non-image files).
     *
     * @param files List of files to be checked.
     * @return List of valid image files (PNG, JPG, JPEG).
     */
    private List<File> filterValidImageFiles(List<File> files) {
        List<File> validImages = new ArrayList<>();
        for (File file : files) {
            if (isValidImageFile(file)) {
                validImages.add(file);  // Add valid image files to the list
            } else {
                logger.warn("Invalid file type: {}", file.getName());  // Log invalid files
            }
        }
        return validImages;
    }

    /**
     * Checks if a file is a valid image file (PNG, JPG, or JPEG).
     *
     * @param file The file to check.
     * @return True if the file has a valid image extension.
     */
    private boolean isValidImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
    }

    /**
     * Opens a file chooser to allow the user to select multiple image files.
     */
    private void chooseFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image Files");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(browseButton.getScene().getWindow());

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            logger.info("User selected files: {}", selectedFiles);
            processValidFiles(selectedFiles);
        }
    }

    /**
     * Processes the valid image files by performing OCR or any other operations.
     *
     * @param validImageFiles The list of valid image files.
     */
    private void processValidFiles(List<File> validImageFiles) {
        Platform.runLater(() -> progressIndicator.setVisible(true));

        new Thread(() -> {
            ExecutorService executorService = Executors.newFixedThreadPool(4);

            List<Future<?>> futures = new ArrayList<>();
            List<QuizResult> quizResults = Collections.synchronizedList(new ArrayList<>());

            // Pour chaque fichier, soumettre une tâche au pool
            for (File file : validImageFiles) {
                QuizTask task = new QuizTask(file, availableFolders, ocrService, quizService, userService, logger, quizResults);
                futures.add(executorService.submit(task));
            }

            // Attendre que toutes les tâches soient terminées
            for (Future<?> future : futures) {
                try {
                    future.get(); // Bloque jusqu'à ce que la tâche soit terminée

                } catch (ExecutionException e) {
                    logger.error("Error processing task", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Error processing task", e);
                }
            }

            // Arrêter le pool de threads de manière ordonnée
            executorService.shutdown();

            // Une fois le traitement terminé, retourner sur le thread UI pour masquer l'indicateur et afficher les alertes
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                showNextQuizAlert(quizResults, 0);
            });
        }).start();
    }



    /**
     * Affiche l'alerte pour le quiz situé à l'index 'index' dans la liste et,
     * lorsque la réponse est traitée, appelle récursivement l'affichage pour le quiz suivant.
     *
     * @param quizResults La liste de tous les résultats de quiz.
     * @param index       L'index du quiz à afficher.
     */
    private void showNextQuizAlert(List<QuizResult> quizResults, int index) {
        if (index >= quizResults.size()) return;

        QuizResult currentResult = quizResults.get(index);
        Alert alert = createQuizAlert(currentResult.getQuestion(), currentResult.getAnswer());
        Optional<ButtonType> resultAlert = alert.showAndWait();

        if (resultAlert.isEmpty() || resultAlert.get().getText().equals("Cancel")) {
            logger.info("User canceled quiz creation for file {}", currentResult.getFile().getName());
            showNextQuizAlert(quizResults, index + 1);
            return;
        }

        if (resultAlert.get().getText().equals("Generate Again")) {
            logger.info("User asked to regenerate quiz for file {}", currentResult.getFile().getName());
            regenerateQuiz(quizResults, currentResult, index);
        } else { // Si l'utilisateur choisit "Yes"
            showFolderDialogAndSave(currentResult, () ->
                    // Après la sauvegarde, passer au quiz suivant
                    Platform.runLater(() -> showNextQuizAlert(quizResults, index + 1))
            );
        }
    }

    private void regenerateQuiz(List<QuizResult> quizResults, QuizResult currentResult, int index){
        new Thread(() -> {
            try {
                String text = extractTextFromImage(currentResult.getFile()); //why scan again ?
                if (text == null) {
                    logger.error("No text extracted from file {}", currentResult.getFile().getName());
                    // Passer au quiz suivant même en cas d'erreur
                    Platform.runLater(() -> showNextQuizAlert(quizResults, index + 1));
                    return;
                }
                List<String> newQuiz = GeminiApiService.generateQuizFromText(text);
                String newQuestion = newQuiz.get(0);
                String newAnswer = newQuiz.get(1);
                QuizResult newResult = new QuizResult(currentResult.getFile(), newQuestion, newAnswer);

                // Remplace l'ancien résultat par le nouveau dans la liste
                synchronized (quizResultsLock) {
                    quizResults.set(index, newResult);
                }

                // Réaffiche l'alerte pour le quiz régénéré
                Platform.runLater(() -> showNextQuizAlert(quizResults, index));
            } catch (Exception ex) {
                logger.error("Error regenerating quiz for file: " + currentResult.getFile().getName(), ex);
                // En cas d'erreur, poursuivre avec le quiz suivant
                Platform.runLater(() -> showNextQuizAlert(quizResults, index + 1));
            }
        }).start();
    }
    /**
     * Creates an alert to confirm the generated quiz question and answer.
     *
     * @param question The generated question.
     * @param answer   The generated answer.
     * @return The configured Alert object.
     */
    private Alert createQuizAlert(String question, String answer) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quiz Confirmation");
        alert.setHeaderText("Do you want to keep this quiz?");
        alert.setContentText("Question:\n" + question + "\n\nAnswer:\n" + answer);

        ButtonType yesBtn = new ButtonType("Yes");
        ButtonType regenerateBtn = new ButtonType("Generate Again");
        ButtonType cancelBtn = new ButtonType("Cancel");

        alert.getButtonTypes().setAll(yesBtn, regenerateBtn, cancelBtn);
        return alert;
    }

    /**
     * Affiche une boîte de dialogue pour choisir le dossier et sauvegarde le quiz.
     *
     * @param result Le quiz résultat à sauvegarder.
     * @param onComplete   Une Runnable qui sera exécutée après la sauvegarde (par exemple, pour passer au quiz suivant).
     */
    private void showFolderDialogAndSave(QuizResult result, Runnable onComplete) {
        List<String> folderNames = availableFolders.stream().map(Folder::getName).toList();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(folderNames.get(0), folderNames);
        dialog.setTitle("Choose Folder");
        dialog.setHeaderText("Select a folder to save the quiz");

        Optional<String> selectedFolder = dialog.showAndWait();
        selectedFolder.ifPresent(folderName -> {
            Folder folder = availableFolders.stream()
                    .filter(f -> f.getName().equals(folderName))
                    .findFirst()
                    .orElseThrow();
            quizService.createAndSaveQuiz(result.getQuestion(), result.getAnswer(), userService.getCurrentUser() ,folder);
        });
        onComplete.run();
    }

    private String extractTextFromImage(File file) {
        try {
            return ocrService.scan(file);
        } catch (Exception e) {
            logger.error("Error extracting text from image", e);
            return null;
        }
    }

}