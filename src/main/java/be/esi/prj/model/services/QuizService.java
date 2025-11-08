package be.esi.prj.model.services;

import be.esi.prj.model.orm.Folder;
import be.esi.prj.model.orm.Question;
import be.esi.prj.model.orm.User;
import be.esi.prj.model.repository.QuizRepository;
import be.esi.prj.utils.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

/**
 * Service class for managing quiz-related operations, such as creating, and retrieving questions.
 * This class interacts with the repository layer to perform CRUD operations on questions.
 */
public class QuizService {

    private static QuizService instance;

    private final QuizRepository questionRepository;
    private final EntityManager em;

    private QuizService() {
        this.em = JPAUtil.getEntityManager();
        questionRepository = new QuizRepository(em);
    }

    public QuizService(EntityManager em, QuizRepository repo) {
        this.em = em;
        this.questionRepository = repo;
    }
    /**
     * Returns the singleton instance of the QuizService.
     *
     * @return The single instance of QuizService.
     */
    public static synchronized QuizService getInstance() {
        if (instance == null) {
            instance = new QuizService();
        }
        return instance;
    }

    /**
     * Creates and saves a new quiz question in the database.
     *
     * @param text The question text.
     * @param answer The answer to the question.
     * @param user The user who created the question.
     * @param folder The folder under which the question will be stored.
     */
    public void createAndSaveQuiz(String text, String answer, User user, Folder folder) {
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            Question question = new Question(text, answer, user, folder);
            questionRepository.save(question);
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            throw new RuntimeException("Failed to create and save quiz", e);
        }
    }

    /**
     * Retrieves the list of due questions for a specific user and folder.
     *
     * @param folderId The ID of the folder containing the questions.
     * @param user The user whose due questions are to be retrieved.
     * @return A list of due questions.
     */
    public List<Question> getDueQuestions(int folderId, User user) {
        return questionRepository.getDueQuestions(folderId, user);
    }
}
