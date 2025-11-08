package be.esi.prj.model.services;

import be.esi.prj.model.orm.DifficultyLevel;
import be.esi.prj.model.orm.Question;
import be.esi.prj.model.orm.ReviewHistory;
import be.esi.prj.model.repository.ReviewHistoryRepository;
import be.esi.prj.utils.JPAUtil;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Service class responsible for managing review history and scheduling the next review based on the user's progress.
 */
public class ReviewService {

    private final EntityManager em;
    private final ReviewHistoryRepository reviewRepository;
    private final UserService userService;

    /**
     * Default constructor initializing the EntityManager, Repository and UserService.
     */
    public ReviewService() {
        this.em = JPAUtil.getEntityManager();
        this.reviewRepository = new ReviewHistoryRepository(em);
        this.userService = UserService.getInstance();
    }

    /**
     * Constructor for testing purposes.
     */
    public ReviewService(EntityManager em, ReviewHistoryRepository repository, UserService userService) {
        this.em = em;
        this.reviewRepository = repository;
        this.userService = userService;
    }

    /**
     * Saves or updates the review history for a given question based on the user's progress.
     *
     * @param question The question being reviewed.
     * @param difficulty The difficulty level chosen by the user.
     */
    public void save(Question question, DifficultyLevel difficulty) {
        ReviewHistory existingHistory = reviewRepository.findReviewHistory(question, userService.getCurrentUser());
        NextReviewDetails result = computeNextReviewSchedule(difficulty, existingHistory);

        if (existingHistory != null) {
            updateReviewHistory(existingHistory, difficulty, result, question);
        } else {
            createNewReviewHistory(question, difficulty, result);
        }
    }

    /**
     * Computes the next review schedule based on the difficulty of the review and the user's review history.
     *
     * @param difficulty The difficulty level of the review (e.g., AGAIN, HARD, GOOD, EASY).
     * @param history The user's review history containing the ease factor and previous review intervals.
     * @return A NextReviewDetails object containing the next review date and the updated ease factor.
     */
    private NextReviewDetails computeNextReviewSchedule(DifficultyLevel difficulty, ReviewHistory history) {
        double easeFactor = 2.5;
        int lastInterval = 1;

        if (history != null) {
            easeFactor = history.getEaseFactor();
            lastInterval = (int) ChronoUnit.DAYS.between(history.getReviewDate(), history.getNextReviewDate());
        }

        return determineNextReviewSchedule(difficulty, easeFactor, lastInterval);
    }


    /**
     * Determines the next review date and the updated ease factor based on the difficulty level.
     * It adjusts the ease factor and calculates a new review interval depending on how well the user
     * performed in the previous review.
     *
     * @param difficulty The difficulty level (e.g., AGAIN, HARD, GOOD, EASY).
     * @param easeFactor The current ease factor from previous reviews.
     * @param lastInterval The number of days since the last review.
     * @return A NextReviewDetails object containing the next review date and the updated ease factor.
     */
    private NextReviewDetails determineNextReviewSchedule(DifficultyLevel difficulty, double easeFactor, int lastInterval) {
        LocalDate today = LocalDate.now();
        int newInterval;
        switch (difficulty) {
            case AGAIN:
                newInterval = 0;
                break;

            case HARD:
                easeFactor = Math.max(easeFactor - 0.1, 1.3);
                newInterval = Math.max(2, (int) (lastInterval * 1.2));  // Minimum 2 days
                break;

            case GOOD:
                easeFactor += 0.1;
                newInterval = Math.max(4, (int) (lastInterval * easeFactor));  // Minimum 4 days
                break;

            case EASY:
                easeFactor += 0.2;
                newInterval = Math.max(7, (int) (lastInterval * easeFactor * 1.5));  // Minimum 7 days
                break;

            default:
                newInterval = 0;
        }
        return new NextReviewDetails(today.plusDays(newInterval), easeFactor);
    }

    /**
     * Updates the existing review history with the new data and commits the changes to the database.
     *
     * @param existingHistory The existing review history to update.
     * @param difficulty The difficulty level chosen by the user.
     * @param result The result containing the next review date and updated ease factor.
     * @param question The question being reviewed.
     */
    private void updateReviewHistory(ReviewHistory existingHistory, DifficultyLevel difficulty, NextReviewDetails result, Question question) {
        existingHistory.setReviewDate(LocalDate.now());
        existingHistory.setDifficulty(difficulty);
        existingHistory.setNextReviewDate(result.nextReviewDate());
        existingHistory.setEaseFactor(result.easeFactor());
        question.setDifficulty(difficulty);

        executeTransaction(() -> {
            em.merge(existingHistory);
            em.merge(question);
        });
    }

    /**
     * Creates a new review history entry and commits it to the database.
     *
     * @param question The question being reviewed.
     * @param difficulty The difficulty level chosen by the user.
     * @param result The result containing the next review date and updated ease factor.
     */
    private void createNewReviewHistory(Question question, DifficultyLevel difficulty, NextReviewDetails result) {
        ReviewHistory history = new ReviewHistory();
        history.setUser(userService.getCurrentUser());
        history.setQuestion(question);
        history.setReviewDate(LocalDate.now());
        history.setDifficulty(difficulty);
        history.setNextReviewDate(result.nextReviewDate());
        history.setEaseFactor(result.easeFactor());
        question.setDifficulty(difficulty);

        executeTransaction(() -> {
            reviewRepository.save(history);
            em.merge(question);
        });
    }

    /**
     * Executes a transaction with the provided operation.
     *
     * @param operation The operation to execute within the transaction.
     */
    private void executeTransaction(Runnable operation) {
        em.getTransaction().begin();
        try {
            operation.run();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Transaction failed", e);
        }
    }

    /**
     * Retrieves the remaining review times for a question, based on different difficulty levels.
     *
     * @param question The question to check.
     * @return A map containing the difficulty levels and their respective remaining review times.
     */
    public Map<DifficultyLevel, String> getRemainingReviewTimes(Question question) {
        Map<DifficultyLevel, String> result = new EnumMap<>(DifficultyLevel.class);
        ReviewHistory history = reviewRepository.findReviewHistory(question, userService.getCurrentUser());
        LocalDate today = LocalDate.now();

        for (DifficultyLevel level : List.of(DifficultyLevel.HARD, DifficultyLevel.GOOD, DifficultyLevel.EASY)) {
            NextReviewDetails projected = computeNextReviewSchedule(level, history);
            long days = ChronoUnit.DAYS.between(today, projected.nextReviewDate());
            result.put(level, days == 0 ? "Today" : days + "d");
        }

        return result;
    }

    /**
     * Record to store the next review date and ease factor.
     */
    private record NextReviewDetails(LocalDate nextReviewDate, double easeFactor) {}
}
