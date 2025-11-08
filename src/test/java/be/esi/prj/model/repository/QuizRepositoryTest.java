package be.esi.prj.model.repository;

import be.esi.prj.model.orm.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QuizRepositoryTest {

    private static EntityManagerFactory emf;
    private static EntityManager em;
    private QuizRepository quizRepository;

    private User testUser;
    private Folder testFolder;

    @BeforeAll
    public static void setupClass() {
        emf = Persistence.createEntityManagerFactory("testPU");
        em = emf.createEntityManager();
    }

    @BeforeEach
    public void setup() {
        quizRepository = new QuizRepository(em);

        em.getTransaction().begin();

        testUser = new User("user_" + System.currentTimeMillis(), "password123");
        em.persist(testUser);

        testFolder = new Folder("Folder_" + System.currentTimeMillis(), testUser);
        em.persist(testFolder);

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
    public void testSaveQuestion() {
        Question question = new Question("What is Java?", "A programming language", testUser, testFolder);
        quizRepository.save(question);
        em.flush();

        Question retrieved = em.find(Question.class, question.getQuestionId());
        assertNotNull(retrieved);
        assertEquals("What is Java?", retrieved.getQuestionText());
    }

    @Test
    public void testGetDueQuestions_withNoReviewHistory() {
        Question q1 = new Question("Q1", "Answer 1", testUser, testFolder);
        quizRepository.save(q1);
        em.flush();

        List<Question> due = quizRepository.getDueQuestions(testFolder.getFolderId(), testUser);
        assertNotNull(due);
        assertEquals(1, due.size());
        assertEquals("Q1", due.get(0).getQuestionText());
    }

    @Test
    public void testGetDueQuestions_withReviewHistoryInFuture() {
        Question q2 = new Question("Q2", "Answer 2", testUser, testFolder);
        quizRepository.save(q2);
        em.flush();

        ReviewHistory rh = new ReviewHistory(
                testUser,
                q2,
                DifficultyLevel.GOOD,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                2.5
        );
        em.persist(rh);
        em.flush();

        List<Question> due = quizRepository.getDueQuestions(testFolder.getFolderId(), testUser);
        assertTrue(due.isEmpty(), "The question shouldn't be due yet");
    }

    @Test
    public void testGetDueQuestions_withReviewHistoryInPast() {
        Question q3 = new Question("Q3", "Answer 3", testUser, testFolder);
        quizRepository.save(q3);
        em.flush();

        ReviewHistory rh = new ReviewHistory(
                testUser,
                q3,
                DifficultyLevel.HARD,
                LocalDate.now().minusDays(3),
                LocalDate.now().minusDays(1),
                2.2
        );
        em.persist(rh);
        em.flush();

        List<Question> due = quizRepository.getDueQuestions(testFolder.getFolderId(), testUser);
        assertEquals(1, due.size());
        assertEquals("Q3", due.get(0).getQuestionText());
    }


}
