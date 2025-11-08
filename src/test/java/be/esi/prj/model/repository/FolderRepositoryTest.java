package be.esi.prj.model.repository;

import be.esi.prj.model.orm.DifficultyLevel;
import be.esi.prj.model.orm.Folder;
import be.esi.prj.model.orm.Question;
import be.esi.prj.model.orm.User;
import be.esi.prj.model.repository.FolderRepository;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;




public class FolderRepositoryTest {

    private static EntityManagerFactory emf;
    private static EntityManager em;
    private FolderRepository folderRepository;

    private User testUser;

    @BeforeAll
    public static void setupClass() {
        // Créer l'EntityManagerFactory et l'EntityManager avec H2 en mémoire
        emf = Persistence.createEntityManagerFactory("testPU"); // Utiliser le persistence unit "testPU" configuré pour une base en mémoire
        em = emf.createEntityManager();
    }

    @BeforeEach
    public void setup() {
        // Initialiser le repository avant chaque test
        folderRepository = new FolderRepository(em);

        // Créer un nom d'utilisateur unique pour chaque test
        String uniqueUsername = "testUser" + System.currentTimeMillis();

        // Créer un utilisateur unique et l'ajouter à la base
        em.getTransaction().begin();
        testUser = new User(uniqueUsername, "password123");
        em.persist(testUser);
        em.getTransaction().commit();

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
    public void testSaveFolder() {
        Folder folder = new Folder("Folder 1", testUser);
        folderRepository.save(folder);
        em.flush();

        // Vérifier que le dossier a bien été enregistré
        Folder retrievedFolder = folderRepository.findById(folder.getFolderId());
        assertNotNull(retrievedFolder);
        assertEquals("Folder 1", retrievedFolder.getName());
        assertEquals(testUser.getUsername(), retrievedFolder.getUser().getUsername());
    }

    @Test
    public void testFindByUser() {
        Folder folder1 = new Folder("Folder 1", testUser);
        Folder folder2 = new Folder("Folder 2", testUser);
        folderRepository.save(folder1);
        folderRepository.save(folder2);
        em.flush();

        // Récupérer les dossiers associés à testUser
        List<Folder> folders = folderRepository.findByUser(testUser);
        assertNotNull(folders);
        assertEquals(2, folders.size());
    }

    @Test
    public void testDeleteFolder() {
        Folder folder = new Folder("Folder to Delete", testUser);
        folderRepository.save(folder);
        em.flush();

        // Supprimer le dossier
        folderRepository.delete(folder);
        em.flush();

        // Vérifier que le dossier n'existe plus dans la base
        Folder deletedFolder = folderRepository.findById(folder.getFolderId());
        assertNull(deletedFolder);
    }

    @Test
    public void testFindById_whenFolderExists_returnsFolder() {
        Folder folder = new Folder("Folder 1", testUser);
        folderRepository.save(folder);
        em.flush();

        // Récupérer le dossier par son ID
        Folder retrievedFolder = folderRepository.findById(folder.getFolderId());
        assertNotNull(retrievedFolder);
        assertEquals("Folder 1", retrievedFolder.getName());
    }

    @Test
    public void testFindById_whenFolderDoesNotExist_returnsNull() {
        // Essayer de récupérer un dossier qui n'existe pas
        Folder retrievedFolder = folderRepository.findById(999); // ID qui n'existe pas
        assertNull(retrievedFolder);
    }


    @Test
    public void testGetTotalQuestionsCount() {
        Folder folder = new Folder("Folder with questions", testUser);
        folderRepository.save(folder);

        Question q1 = new Question("Q1", "A1", testUser, folder);
        Question q2 = new Question("Q2", "A2", testUser, folder);

        em.persist(q1);
        em.persist(q2);
        em.flush();

        int count = folderRepository.getTotalQuestionsCount(folder.getFolderId());
        assertEquals(2, count);
    }

    @Test
    public void testGetQuestionsCountByDifficulty() {
        Folder folder = new Folder("Folder with difficulty", testUser);
        folderRepository.save(folder);

        Question q1 = new Question("Q1", "A1", testUser, folder);
        q1.setDifficulty(DifficultyLevel.EASY);
        Question q2 = new Question("Q2", "A2", testUser, folder);
        q2.setDifficulty(DifficultyLevel.HARD);

        em.persist(q1);
        em.persist(q2);
        em.flush();

        int easyCount = folderRepository.getQuestionsCountByDifficulty(folder.getFolderId(), DifficultyLevel.EASY);
        int hardCount = folderRepository.getQuestionsCountByDifficulty(folder.getFolderId(), DifficultyLevel.HARD);

        assertEquals(1, easyCount);
        assertEquals(1, hardCount);
    }


}
