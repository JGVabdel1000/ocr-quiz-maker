package be.esi.prj.viewmodel;

import be.esi.prj.model.orm.DifficultyLevel;
import be.esi.prj.model.orm.Question;
import be.esi.prj.model.services.QuizService;
import be.esi.prj.model.services.ReviewService;
import be.esi.prj.model.services.UserService;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class QuizSessionViewModel {
    private final QuizService quizService = QuizService.getInstance();
    private final UserService userService = UserService.getInstance();
    private final ReviewService reviewService = new ReviewService();
    private final Queue<Question> questionQueue = new LinkedList<>();
    private Question currentQuestion;
    private DifficultyLevel selectedDifficulty;

    @FXML private Label questionLabel;
    @FXML private Button showAnswerButton;
    @FXML private ScrollPane answerContainer;
    @FXML private Label answerLabel;
    @FXML private VBox difficultyButtons;
    @FXML private Button nextButton;

    @FXML private Label hardRemainingDays;
    @FXML private Label goodRemainingDays;
    @FXML private Label easyRemainingDays;

    private boolean isAnswerShown = false;
    private final BooleanProperty isDifficultySelected = new SimpleBooleanProperty(false);

    public void initializeWithFolder(int folderId) {
        questionQueue.clear();
        questionQueue.addAll(quizService.getDueQuestions(folderId, userService.getCurrentUser()));

        nextButton.disableProperty().bind(isDifficultySelected.not());
        loadNextQuestion();
    }

    private void loadNextQuestion() {
        if (questionQueue.isEmpty()) {
            showCompletionState();
            return;
        }

        currentQuestion = questionQueue.poll();
        updateIntervalLabels();

        questionLabel.setText(currentQuestion.getQuestionText());
        answerLabel.setText(currentQuestion.getAnswer());
        resetState();
    }

    private void showCompletionState() {
        questionLabel.setText("All questions completed!");
        showAnswerButton.setVisible(false);
        answerContainer.setVisible(false);
        difficultyButtons.setVisible(false);
        nextButton.setVisible(false);

        answerContainer.setManaged(false);
        difficultyButtons.setManaged(false);
        nextButton.setManaged(false);
        showAnswerButton.setManaged(false);
    }

    private void resetState() {
        isAnswerShown = false;
        isDifficultySelected.set(false);
        selectedDifficulty = null;
        questionLabel.setTranslateY(0);
        questionLabel.setScaleX(1);
        questionLabel.setScaleY(1);

        showAnswerButton.setVisible(true);
        showAnswerButton.setManaged(true);
        answerContainer.setVisible(false);
        answerContainer.setManaged(false);

        difficultyButtons.setVisible(false);
        difficultyButtons.setManaged(false);

        nextButton.setVisible(false);
        nextButton.setManaged(false);
    }

    @FXML
    private void onShowAnswerClicked() {
        animateQuestionToTop();
        isAnswerShown = true;

        answerContainer.setVisible(true);
        answerContainer.setManaged(true);

        difficultyButtons.setVisible(true);
        difficultyButtons.setManaged(true);

        nextButton.setVisible(true);
        nextButton.setManaged(true);
        showAnswerButton.setVisible(false);
        showAnswerButton.setManaged(false);
    }

    @FXML
    private void onAgainClicked() {
        selectedDifficulty = DifficultyLevel.AGAIN;
        isDifficultySelected.set(true);
    }

    @FXML
    private void onHardClicked() {
        selectedDifficulty = DifficultyLevel.HARD;
        isDifficultySelected.set(true);
    }

    @FXML
    private void onGoodClicked() {
        selectedDifficulty = DifficultyLevel.GOOD;
        isDifficultySelected.set(true);
    }

    @FXML
    private void onEasyClicked() {
        selectedDifficulty = DifficultyLevel.EASY;
        isDifficultySelected.set(true);
    }


    @FXML
    private void onNextClicked() {
        reviewService.save(currentQuestion, selectedDifficulty);
        if (selectedDifficulty != DifficultyLevel.EASY) {
            questionQueue.offer(currentQuestion);
        }
        loadNextQuestion();
    }

    private void updateIntervalLabels() {
        Map<DifficultyLevel, String> intervals = reviewService.getRemainingReviewTimes(currentQuestion);

        hardRemainingDays.setText(intervals.get(DifficultyLevel.HARD));
        goodRemainingDays.setText(intervals.get(DifficultyLevel.GOOD));
        easyRemainingDays.setText(intervals.get(DifficultyLevel.EASY));
    }

    private void animateQuestionToTop() {
        TranslateTransition transition = new TranslateTransition(Duration.millis(300), questionLabel);
        transition.setByY(-20);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), questionLabel);
        scale.setToX(0.95);
        scale.setToY(0.95);

        ParallelTransition parallel = new ParallelTransition(transition, scale);
        parallel.play();
    }
}