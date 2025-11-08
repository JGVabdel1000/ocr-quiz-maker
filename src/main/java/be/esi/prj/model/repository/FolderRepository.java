package be.esi.prj.model.repository;

import be.esi.prj.model.orm.DifficultyLevel;
import be.esi.prj.model.orm.Folder;
import be.esi.prj.model.orm.User;

import jakarta.persistence.*;
import java.util.List;

/**
 * Repository for accessing and manipulating Folder entities.
 */
public class FolderRepository {

    private final EntityManager em;

    public FolderRepository(EntityManager em) {
        this.em = em;
    }

    public void save(Folder folder) {
        em.persist(folder);
    }

    /**
     * Retrieves all folders owned by a specific user.
     */
    public List<Folder> findByUser(User user) {
        TypedQuery<Folder> query = em.createQuery(
                "SELECT f FROM Folder f WHERE f.user = :user", Folder.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    /**
     * Deletes the given folder from the database.
     */
    public void delete(Folder folder) {
        Folder managed = em.merge(folder);
        em.remove(managed);
    }

    /**
     * Finds a folder by its ID.
     */
    public Folder findById(int folderId) {
        return em.find(Folder.class, folderId);
    }

    /**
     * Returns the total number of questions in a folder.
     */
    public int getTotalQuestionsCount(int folderId) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(q) FROM Question q WHERE q.folder.folderId = :folderId", Long.class);
        query.setParameter("folderId", folderId);
        Long count = query.getSingleResult();
        return count != null ? count.intValue() : 0;
    }

    /**
     * Returns the number of questions of a specific difficulty in a folder.
     */
    public int getQuestionsCountByDifficulty(int folderId, DifficultyLevel difficulty) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(q) FROM Question q WHERE q.folder.folderId = :folderId AND q.difficulty = :difficulty", Long.class);
        query.setParameter("folderId", folderId);
        query.setParameter("difficulty", difficulty);
        Long count = query.getSingleResult();
        return count != null ? count.intValue() : 0;
    }
}
