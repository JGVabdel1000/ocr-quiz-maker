package be.esi.prj.model.services;

import be.esi.prj.model.orm.Folder;
import be.esi.prj.model.orm.Question;
import be.esi.prj.model.orm.User;
import be.esi.prj.model.repository.QuizRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuizServiceTest {

    private QuizService quizService;
    private QuizRepository mockRepository;
    private EntityManager mockEm;
    private EntityTransaction mockTransaction;

    @BeforeEach
    void setUp() {
        mockEm = mock(EntityManager.class);
        mockRepository = mock(QuizRepository.class);
        mockTransaction = mock(EntityTransaction.class);

        when(mockEm.getTransaction()).thenReturn(mockTransaction);
        quizService = new QuizService(mockEm, mockRepository);
    }

    @Test
    void createAndSaveQuiz_shouldCallRepositorySave() {
        // Arrange
        User user = new User();
        Folder folder = new Folder();
        String questionText = "Quelle est la capitale de la France ?";
        String answerText = "Paris";

        // Act
        quizService.createAndSaveQuiz(questionText, answerText, user, folder);

        // Assert
        verify(mockRepository, times(1)).save(any(Question.class));
    }

    @Test
    void getDueQuestions_shouldReturnListFromRepository() {
        // Arrange
        User user = new User();
        List<Question> expectedQuestions = Arrays.asList(new Question(), new Question());

        when(mockRepository.getDueQuestions(anyInt(), any(User.class))).thenReturn(expectedQuestions);

        // Act
        List<Question> result = quizService.getDueQuestions(1, user);

        // Assert
        assertEquals(2, result.size());
        assertEquals(expectedQuestions, result);
    }

    @Test
    void createAndSaveQuiz_shouldRollbackTransaction_whenExceptionOccurs() {
        User user = new User();
        Folder folder = new Folder();

        doThrow(new RuntimeException("DB error")).when(mockRepository).save(any(Question.class));
        when(mockTransaction.isActive()).thenReturn(true); // important pour déclencher rollback

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                quizService.createAndSaveQuiz("Q", "A", user, folder)
        );

        assertTrue(ex.getMessage().contains("Failed to create and save quiz"));
        verify(mockTransaction).rollback(); // Vérifie que le rollback a été déclenché
    }

}