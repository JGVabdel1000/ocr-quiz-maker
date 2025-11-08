package be.esi.prj.model.repository;

import be.esi.prj.model.orm.User;
import be.esi.prj.utils.JPAUtil;
import jakarta.persistence.*;

/**
 * Repository class for managing User entities.
 */
public class UserRepository {

    private EntityManager em;

    public UserRepository(EntityManager em) {
        this.em = em;
    }

    /**
     * Checks if a user with the given username exists in the database.
     *
     * @param userName the username to check for existence.
     * @return {@code true} if a user with the given username exists, {@code false} otherwise.
     */
    public boolean existsByUserName(String userName) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :userName", Long.class);
        query.setParameter("userName", userName);
        return query.getSingleResult() > 0;
    }

    /**
     * Retrieves a user by their username.
     *
     * @param userName the username of the user to be retrieved.
     * @return the User entity if found.
     * @throws RepositoryException if no user is found with the given username.
     */
    public User getUserByUsername(String userName) {
        TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.username = :userName", User.class);
        query.setParameter("userName", userName);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            throw new RepositoryException("No user with userName: "+userName+" found", e.getCause());
        }
    }

    public void save(User user) {
        em.persist(user);
    }
}
