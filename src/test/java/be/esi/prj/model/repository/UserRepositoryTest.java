package be.esi.prj.model.repository;

import static org.junit.jupiter.api.Assertions.*;
import be.esi.prj.model.orm.User;
import be.esi.prj.model.repository.RepositoryException;
import be.esi.prj.model.repository.UserRepository;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest {

    private static EntityManagerFactory emf;
    private static EntityManager em;
    private UserRepository repository;

    @BeforeAll
    public static void setupClass() {
        // Créer l'EntityManagerFactory et l'EntityManager avec H2 en mémoire
        emf = Persistence.createEntityManagerFactory("testPU");  // Utiliser le persistence unit "testPU" configuré pour une base en mémoire
        em = emf.createEntityManager();
    }

    @BeforeEach
    public void setup() {
        // Initialiser le repository avant chaque test
        repository = new UserRepository(em);
        // Démarrer une transaction pour chaque test
        em.getTransaction().begin();
    }

    @AfterEach
    public void tearDown() {
        // Annuler la transaction après chaque test
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }

    @AfterAll
    public static void tearDownClass() {
        // Fermer l'EntityManager et l'EntityManagerFactory après tous les tests
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    public void testSaveUser() {
        User user = new User("alice", "password123");
        repository.save(user);
        em.flush();  // Assurer que l'objet est persisté avant d'effectuer les vérifications

        // Vérifier si l'utilisateur a bien été sauvegardé
        assertTrue(repository.existsByUserName("alice"));
    }

    @Test
    public void testExistsByUserName_whenUserExists_returnsTrue() {
        User user = new User("bob", "password123");
        repository.save(user);
        em.flush();

        // Vérifier si l'utilisateur existe
        assertTrue(repository.existsByUserName("bob"));
    }

    @Test
    public void testExistsByUserName_whenUserDoesNotExist_returnsFalse() {
        // Vérifier que l'utilisateur n'existe pas
        assertFalse(repository.existsByUserName("nonexistent"));
    }

    @Test
    public void testGetUserByUsername_whenUserExists_returnsUser() {
        User user = new User("charlie", "mypassword");
        repository.save(user);
        em.flush();

        // Récupérer l'utilisateur par son nom d'utilisateur
        User fetchedUser = repository.getUserByUsername("charlie");

        assertNotNull(fetchedUser);
        assertEquals("charlie", fetchedUser.getUsername());
        assertEquals("mypassword", fetchedUser.getPassword());
    }

    @Test
    public void testGetUserByUsername_whenUserDoesNotExist_throwsException() {
        // Vérifier qu'une exception est lancée quand l'utilisateur n'existe pas
        RepositoryException exception = assertThrows(RepositoryException.class, () -> {
            repository.getUserByUsername("ghost");
        });

        assertTrue(exception.getMessage().contains("No user with userName"));
    }

    @Test
    public void testSaveDuplicateUser_throwsException() {
        User user1 = new User("duplicate", "password123");
        repository.save(user1);
        em.flush();

        // Tentative de sauvegarder un utilisateur avec un nom d'utilisateur existant
        User user2 = new User("duplicate", "anotherpassword");

        // Une exception devrait être levée (en supposant que le code gère les duplicatas correctement)
        assertThrows(PersistenceException.class, () -> {
            repository.save(user2);
            em.flush();
        });
    }
}
