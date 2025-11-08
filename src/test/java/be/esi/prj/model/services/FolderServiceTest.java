package be.esi.prj.model.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import be.esi.prj.model.orm.DifficultyLevel;
import be.esi.prj.model.orm.Folder;
import be.esi.prj.model.orm.User;
import be.esi.prj.model.repository.FolderRepository;
import be.esi.prj.model.repository.RepositoryException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FolderServiceTest {

    private FolderService folderService;
    private EntityManager em;
    private EntityTransaction tx;
    private FolderRepository folderRepo;

    private User user;
    private Folder folder;

    @BeforeEach
    public void setup() {
        // mocks
        em = mock(EntityManager.class);
        tx = mock(EntityTransaction.class);
        when(em.getTransaction()).thenReturn(tx);

        folderRepo = mock(FolderRepository.class);

        // SUT
        folderService = new FolderService(em, folderRepo);

        // données communes
        user = new User("u1", "pass");
        folder = new Folder("F1", user);
        folder.setName("F1"); // existant
    }

    @Test
    public void testCreateFolder_success() {
        // on simule que save() ne jette pas
        doNothing().when(folderRepo).save(any(Folder.class));

        Folder created = folderService.createFolder("MyFolder", user);

        // vérifie qu’on a bien démarré/commit la transaction
        verify(tx).begin();
        verify(folderRepo).save(created);
        verify(tx).commit();

        assertEquals("MyFolder", created.getName());
        assertSame(user, created.getUser());
    }

    @Test
    public void testCreateFolder_failure_rollsBackAndThrows() {
        // on simule que save() jette
        doThrow(new RuntimeException("oops"))
                .when(folderRepo).save(any());

        // IMPORTANT : dire que la transaction est active au moment du rollback
        when(tx.isActive()).thenReturn(true);

        RepositoryException ex = assertThrows(
                RepositoryException.class,
                () -> folderService.createFolder("X", user)
        );

        // on doit avoir bien démarré, rollbacké et jeté l'exception
        verify(tx).begin();
        verify(tx).rollback();
        assertTrue(ex.getMessage().contains("Failed to create folder"));
    }


    @Test
    public void testDeleteFolder_existingFolder() {
        when(folderRepo.findById(42)).thenReturn(folder);
        doNothing().when(folderRepo).delete(folder);

        folderService.deleteFolder(42);

        verify(tx).begin();
        verify(folderRepo).findById(42);
        verify(folderRepo).delete(folder);
        verify(tx).commit();
    }

    @Test
    public void testDeleteFolder_notExistingFolder() {
        when(folderRepo.findById(99)).thenReturn(null);

        folderService.deleteFolder(99);

        verify(tx).begin();
        verify(folderRepo).findById(99);
        verify(folderRepo, never()).delete(any());
        verify(tx).commit();
    }

    @Test
    public void testUpdateFolder_success() {
        Folder modified = new Folder("NewName", user);
        modified.setName("NewName");

        folderService.updateFolder(modified);

        verify(tx).begin();
        verify(em).merge(modified);
        verify(tx).commit();
    }

    @Test
    public void testUpdateFolder_failure_rollsBack() {
        Folder f = new Folder("N", user);
        // Stub pour que merge(f) jette
        doThrow(new RuntimeException()).when(em).merge(f);

        // IMPORTANT : dire que la transaction est active au moment du catch
        when(tx.isActive()).thenReturn(true);

        // On s’attend à une exception
        assertThrows(RepositoryException.class, () -> folderService.updateFolder(f));

        // Vérifie bien begin + rollback
        verify(tx).begin();
        verify(tx).rollback();
    }


    @Test
    public void testGetFoldersForUser_delegatesToRepo() {
        List<Folder> list = Arrays.asList(folder);
        when(folderRepo.findByUser(user)).thenReturn(list);

        List<Folder> result = folderService.getFoldersForUser(user);

        assertEquals(list, result);
        verify(folderRepo).findByUser(user);
    }

    @Test
    public void testStatistics_delegation() {
        when(folderRepo.getTotalQuestionsCount(1)).thenReturn(5);
        when(folderRepo.getQuestionsCountByDifficulty(1, DifficultyLevel.EASY)).thenReturn(1);
        when(folderRepo.getQuestionsCountByDifficulty(1, DifficultyLevel.GOOD)).thenReturn(2);
        when(folderRepo.getQuestionsCountByDifficulty(1, DifficultyLevel.HARD)).thenReturn(3);
        when(folderRepo.getQuestionsCountByDifficulty(1, DifficultyLevel.AGAIN)).thenReturn(4);

        assertEquals(5, folderService.getTotalQuestionsCount(1));
        assertEquals(1, folderService.getEasyQuestionsCount(1));
        assertEquals(2, folderService.getGoodQuestionsCount(1));
        assertEquals(3, folderService.getHardQuestionsCount(1));
        assertEquals(4, folderService.getAgainQuestionsCount(1));
    }
}
