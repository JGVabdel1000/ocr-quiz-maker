package be.esi.prj.model.orm;

import jakarta.persistence.*;
import java.util.List;

/**
 * Represents a folder that groups questions for a specific user.
 */
@Entity
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int folderId;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "folder", orphanRemoval = true)
    private List<Question> questions;

    /**
     * Default constructor required by JPA.
     */
    public Folder() {}

    public Folder(String name, User user) {
        this.name = name;
        this.user = user;
    }

    public int getFolderId() {
        return folderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }
}
