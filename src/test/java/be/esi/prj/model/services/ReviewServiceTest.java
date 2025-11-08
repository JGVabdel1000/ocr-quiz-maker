package be.esi.prj.model.services;

import be.esi.prj.model.orm.*;
import be.esi.prj.model.repository.ReviewHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReviewServiceTest {

    @Mock private EntityManager em;
    @Mock private EntityTransaction transaction;
    @Mock private ReviewHistoryRepository reviewRepository;
    @Mock private UserService userService;

    @InjectMocks
    private ReviewService reviewService;

    // permet d'intercepter l'objet passé à une méthode mockée
    @Captor
    private ArgumentCaptor<ReviewHistory> historyCaptor;

    private User mockUser;
    private Question mockQuestion;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockUser = new User();
        mockUser.setUserId(1);
        mockQuestion = new Question();
        mockQuestion.setQuestionId(100L);

        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(em.getTransaction()).thenReturn(transaction);
    }

    @Test
    public void testSave_NewReviewHistory_CreatedAndSaved() {
        when(reviewRepository.findReviewHistory(mockQuestion, mockUser)).thenReturn(null);

        reviewService.save(mockQuestion, DifficultyLevel.GOOD);

        verify(reviewRepository).save(historyCaptor.capture());

        ReviewHistory saved = historyCaptor.getValue();
        assertEquals(mockUser, saved.getUser());
        assertEquals(mockQuestion, saved.getQuestion());
        assertEquals(DifficultyLevel.GOOD, saved.getDifficulty());
        assertEquals(LocalDate.now(), saved.getReviewDate());
        assertTrue(saved.getNextReviewDate().isAfter(LocalDate.now()));
    }

    @Test
    public void testSave_ExistingReviewHistory_UpdatedAndMerged() {
        ReviewHistory existing = new ReviewHistory();
        existing.setReviewDate(LocalDate.now().minusDays(3));
        existing.setNextReviewDate(LocalDate.now());
        existing.setEaseFactor(2.5);
        existing.setDifficulty(DifficultyLevel.HARD);

        when(reviewRepository.findReviewHistory(mockQuestion, mockUser)).thenReturn(existing);

        reviewService.save(mockQuestion, DifficultyLevel.EASY);

        verify(em).merge(existing);
        assertEquals(DifficultyLevel.EASY, existing.getDifficulty());
        assertEquals(LocalDate.now(), existing.getReviewDate());
        assertTrue(existing.getNextReviewDate().isAfter(LocalDate.now()));
    }

    @Test
    void testSave_TransactionFails_ShouldRollback() {
        when(reviewRepository.findReviewHistory(mockQuestion, mockUser)).thenReturn(null);
        doThrow(RuntimeException.class).when(reviewRepository).save(any());

        assertThrows(RuntimeException.class, () ->
                reviewService.save(mockQuestion, DifficultyLevel.GOOD)
        );

        verify(transaction).rollback();
    }


    @Test
    public void testSave_NewReviewHistory_CreatedAndSaved_GoodDifficulty() {
        when(reviewRepository.findReviewHistory(mockQuestion, mockUser)).thenReturn(null);

        reviewService.save(mockQuestion, DifficultyLevel.GOOD);

        ArgumentCaptor<ReviewHistory> captor = ArgumentCaptor.forClass(ReviewHistory.class);
        verify(reviewRepository).save(captor.capture());

        ReviewHistory saved = captor.getValue();
        assertEquals(mockUser, saved.getUser());
        assertEquals(mockQuestion, saved.getQuestion());
        assertEquals(DifficultyLevel.GOOD, saved.getDifficulty());

        LocalDate expectedDate = LocalDate.now().plusDays(4); // based on (difficulty = GOOD) logic
        assertEquals(expectedDate, saved.getNextReviewDate());
    }

    @Test
    public void testSave_ExistingReviewHistory_UpdatedAndMerged_HardDifficulty() {
        ReviewHistory existing = new ReviewHistory();
        existing.setReviewDate(LocalDate.now().minusDays(3));
        existing.setNextReviewDate(LocalDate.now());
        existing.setEaseFactor(2.5);

        when(reviewRepository.findReviewHistory(mockQuestion, mockUser)).thenReturn(existing);

        reviewService.save(mockQuestion, DifficultyLevel.HARD);

        assertEquals(DifficultyLevel.HARD, existing.getDifficulty());
        assertEquals(LocalDate.now(), existing.getReviewDate());
        assertTrue(existing.getNextReviewDate().isAfter(LocalDate.now())); // interval > 0

        verify(em).merge(existing);
        verify(em.getTransaction()).commit();
    }

    @Test
    void testGetRemainingReviewTimes_WithHistory_ReturnsCorrectDays() {
        Question question = new Question();
        User user = new User();
        when(userService.getCurrentUser()).thenReturn(user);

        ReviewHistory history = new ReviewHistory();
        history.setReviewDate(LocalDate.now().minusDays(2));  // reviewed 2 days ago
        history.setNextReviewDate(LocalDate.now());           // scheduled for today
        history.setEaseFactor(2.5);                            // default ease

        when(reviewRepository.findReviewHistory(question, user)).thenReturn(history);

        // When
        Map<DifficultyLevel, String> result = reviewService.getRemainingReviewTimes(question);

        // Then
        assertEquals("2d", result.get(DifficultyLevel.HARD));
        assertEquals("5d", result.get(DifficultyLevel.GOOD));
        assertEquals("8d", result.get(DifficultyLevel.EASY));
    }

}
