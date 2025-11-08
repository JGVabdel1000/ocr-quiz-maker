package be.esi.prj.model.repository;


import be.esi.prj.model.orm.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReviewHistoryRepositoryTest {

    private static EntityManagerFactory emf;
    private static EntityManager em;
    private ReviewHistoryRepository reviewHistoryRepository;
    private QuizRepository quizRepository;

    private User testUser;
    private Folder testFolder;
    private Question testQuestion;

    @BeforeAll
    public static void setupClass() {
        emf = Persistence.createEntityManagerFactory("testPU");
        em = emf.createEntityManager();
    }

    @BeforeEach
    public void setup() {
        reviewHistoryRepository = new ReviewHistoryRepository(em);
        quizRepository = new QuizRepository(em);

        em.getTransaction().begin();

        testUser = new User("user_" + System.currentTimeMillis(), "password123");
        em.persist(testUser);

        testFolder = new Folder("Folder_" + System.currentTimeMillis(), testUser);
        em.persist(testFolder);

        testQuestion = new Question("What is JPA?", "A persistence API", testUser, testFolder);
        quizRepository.save(testQuestion);

        em.getTransaction().commit();
        em.getTransaction().begin();
    }

    @AfterEach
    public void tearDown() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }

    @AfterAll
    public static void tearDownClass() {
        if (em != null && em.isOpen()) em.close();
        if (emf != null && emf.isOpen()) emf.close();
    }

    @Test
    public void testSaveReviewHistory() {
        ReviewHistory rh = new ReviewHistory(
                testUser,
                testQuestion,
                DifficultyLevel.GOOD,
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                2.5
        );

        reviewHistoryRepository.save(rh);
        em.flush();

        ReviewHistory persisted = em.find(ReviewHistory.class, rh.getReviewId());
        assertNotNull(persisted);
        assertEquals(testUser.getUsername(), persisted.getUser().getUsername());
        assertEquals(testQuestion.getQuestionText(), persisted.getQuestion().getQuestionText());
    }

    @Test
    public void testFindReviewHistory_whenExists() {
        ReviewHistory rh = new ReviewHistory(
                testUser,
                testQuestion,
                DifficultyLevel.EASY,
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                2.0
        );
        em.persist(rh);
        em.flush();

        ReviewHistory found = reviewHistoryRepository.findReviewHistory(testQuestion, testUser);
        assertNotNull(found);
        assertEquals(DifficultyLevel.EASY, found.getDifficulty());
    }

    @Test
    public void testFindReviewHistory_whenNotExists_returnsNull() {
        ReviewHistory found = reviewHistoryRepository.findReviewHistory(testQuestion, testUser);
        assertNull(found);
    }
}
