package be.esi.prj.model.orm;

import jakarta.persistence.*;

import java.util.List;

/**
 * Represents a user in the system.
 * <p>
 * This class is a JPA entity that maps to the 'User' table in the database.
 * It includes fields for the user ID, username, and password.
 * </p>
 */
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "user", orphanRemoval = true)
    private List<Folder> folders;

    @OneToMany(mappedBy = "user", orphanRemoval = true)
    private List<ReviewHistory> reviewHistory;

    /**
     * Default constructor required by JPA.
     */
    public User() {}

    /**
     * Constructs a User entity with the specified username and password.
     *
     * @param username the username of the user
     * @param password the encrypted password of the user
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Gets the password of the user.
     *
     * @return the encrypted password of the user
     */
    public String getPassword() {
        return password;
    }

    // for test use
    public void setUserId(int userId) {
        this.userId = userId;
    }
}
