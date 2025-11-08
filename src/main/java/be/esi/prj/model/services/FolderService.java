package be.esi.prj.model.services;

import be.esi.prj.model.orm.DifficultyLevel;
import be.esi.prj.model.orm.Folder;
import be.esi.prj.model.orm.User;
import be.esi.prj.model.repository.FolderRepository;
import be.esi.prj.model.repository.RepositoryException;
import be.esi.prj.utils.JPAUtil;
import jakarta.persistence.*;

import java.util.List;

public class FolderService {

    private final EntityManager em;
    private final FolderRepository folderRepo;

    public FolderService() {
        this.em = JPAUtil.getEntityManager();
        this.folderRepo = new FolderRepository(em);
    }

    /**
     * Creates and persists a new folder.
     *
     * @param folderName the name of the folder
     * @param user the owner of the folder
     * @return the created Folder
     */
    public Folder createFolder(String folderName, User user) {
        Folder folder = new Folder(folderName, user);
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();
            folderRepo.save(folder);
            transaction.commit();
            return folder;
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            throw new RepositoryException("Failed to create folder", e);
        }
    }

    /**
     * Deletes a folder by its ID.
     *
     * @param folderId the folder ID to delete
     */
    public void deleteFolder(int folderId) {
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();
            Folder folder = folderRepo.findById(folderId);
            if (folder != null) {
                folderRepo.delete(folder);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            throw new RepositoryException("Failed to delete folder", e);
        }
    }

    /**
     * Updates a folder's data in the database.
     *
     * @param folder the modified folder to update
     */
    public void updateFolder(Folder folder) {
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            em.merge(folder);
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            throw new RepositoryException("Failed to update folder", e);
        }
    }

    /**
     * Returns all folders associated with a user.
     *
     * @param user the user
     * @return a list of folders
     */
    public List<Folder> getFoldersForUser(User user) {
        return folderRepo.findByUser(user);
    }



    public FolderService(EntityManager em, FolderRepository folderRepo) {
        this.em = em;
        this.folderRepo = folderRepo;
    }

    // --- Statistics ---

    public int getTotalQuestionsCount(int folderId) {
        return folderRepo.getTotalQuestionsCount(folderId);
    }

    public int getEasyQuestionsCount(int folderId) {
        return folderRepo.getQuestionsCountByDifficulty(folderId, DifficultyLevel.EASY);
    }

    public int getGoodQuestionsCount(int folderId) {
        return folderRepo.getQuestionsCountByDifficulty(folderId, DifficultyLevel.GOOD);
    }

    public int getHardQuestionsCount(int folderId) {
        return folderRepo.getQuestionsCountByDifficulty(folderId, DifficultyLevel.HARD);
    }

    public int getAgainQuestionsCount(int folderId) {
        return folderRepo.getQuestionsCountByDifficulty(folderId, DifficultyLevel.AGAIN);
    }
}
