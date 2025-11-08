package be.esi.prj.model.orm;

import jakarta.persistence.*;
import java.util.List;

/**
 * Represents a question associated with a folder and user.
 * Includes the answer, difficulty level, and related review history.
 */
@Entity
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @Column(nullable = false)
    private String questionText;

    @Column(nullable = false)
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel difficulty = DifficultyLevel.HARD;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne
    @JoinColumn(name = "folderId")
    private Folder folder;

    @OneToMany(mappedBy = "question", orphanRemoval = true)
    private List<ReviewHistory> reviewHistory;

    /**
     * Default constructor required by JPA.
     */
    public Question() {}

    public Question(String question, String answer, User user, Folder folder) {
        this.questionText = question;
        this.answer = answer;
        this.user = user;
        this.folder = folder;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getAnswer() {
        return answer;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    // for test
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getQuestionId() {
        return questionId;
    }
}

