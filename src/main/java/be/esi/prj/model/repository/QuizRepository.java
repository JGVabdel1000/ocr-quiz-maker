package be.esi.prj.model.repository;

import be.esi.prj.model.orm.Question;
import be.esi.prj.model.orm.User;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for managing questions related to quiz sessions.
 */
public class QuizRepository {

    private final EntityManager em;

    public QuizRepository(EntityManager em) {
        this.em = em;
    }

    public void save(Question question) {
        em.persist(question);
    }

    /**
     * Retrieves due questions for review in a specific folder and user.
     */
    public List<Question> getDueQuestions(int folderId, User user) {
        String jpql = """
            SELECT q FROM Question q
            LEFT JOIN ReviewHistory rh
              ON rh.question = q AND rh.user = :user
            WHERE q.folder.id = :folderId
              AND q.user.id = :userId
              AND (rh IS NULL OR rh.nextReviewDate <= :today)
        """;

        return em.createQuery(jpql, Question.class)
                .setParameter("folderId", folderId)
                .setParameter("userId", user.getUserId())
                .setParameter("user", user)
                .setParameter("today", LocalDate.now())
                .getResultList();
    }
}
