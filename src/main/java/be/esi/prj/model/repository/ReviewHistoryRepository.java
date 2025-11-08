package be.esi.prj.model.repository;

import be.esi.prj.model.orm.Question;
import be.esi.prj.model.orm.ReviewHistory;
import be.esi.prj.model.orm.User;
import jakarta.persistence.EntityManager;

/**
 * Repository for managing ReviewHistory entries.
 */
public class ReviewHistoryRepository {

    private final EntityManager em;

    public ReviewHistoryRepository(EntityManager em) {
        this.em = em;
    }

    /**
     * Persists a review history record.
     */
    public void save(ReviewHistory reviewHistory) {
        em.persist(reviewHistory);
    }

    /**
     * Finds review history for a question by a specific user.
     *
     * @return the review history or null if not found
     */
    public ReviewHistory findReviewHistory(Question question, User user) {
        String jpql = """
            SELECT rh FROM ReviewHistory rh
            WHERE rh.question = :question
            AND rh.user = :user
            """;
        return em.createQuery(jpql, ReviewHistory.class)
                .setParameter("question", question)
                .setParameter("user", user)
                .getResultStream()
                .findFirst()
                .orElse(null); // If no history exists, return null
    }
}
