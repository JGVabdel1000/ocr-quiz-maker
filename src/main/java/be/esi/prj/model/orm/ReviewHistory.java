package be.esi.prj.model.orm;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entity that tracks the review history of a question for spaced repetition.
 * Stores the review date, next scheduled review, difficulty selected, and ease factor.
 */
@Entity
public class ReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    private LocalDate reviewDate;

    private LocalDate nextReviewDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel difficulty;

    @Column
    private double easeFactor;

    @ManyToOne
    @JoinColumn(name = "user_Id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "question_Id")
    private Question question;

    /**
     * Default constructor required by JPA.
     */
    public ReviewHistory() {}

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public double getEaseFactor() {
        return easeFactor;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    // for tests
    public User getUser() {
        return user;
    }

    // for tests
    public Question getQuestion() {
        return question;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public void setReviewDate(LocalDate reviewDate) {
        this.reviewDate = reviewDate;
    }

    public void setEaseFactor(double easeFactor) {
        this.easeFactor = easeFactor;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public ReviewHistory(User user, Question question, DifficultyLevel difficulty, LocalDate reviewDate, LocalDate nextReviewDate, double easeFactor) {
        this.user = user;
        this.question = question;
        this.difficulty = difficulty;
        this.reviewDate = reviewDate;
        this.nextReviewDate = nextReviewDate;
        this.easeFactor = easeFactor;
    }

}
