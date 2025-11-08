package be.esi.prj.model.services;

import be.esi.prj.model.orm.User;
import be.esi.prj.model.repository.RepositoryException;
import be.esi.prj.model.repository.UserRepository;
import be.esi.prj.utils.JPAUtil;
import jakarta.persistence.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UserService {

    private final EntityManager em;
    private final UserRepository userRepository;
    private static UserService instance;
    private String currentUsername;
    private final StringProperty message = new SimpleStringProperty("");

    public StringProperty messageProperty() {
        return message;
    }
    public String getMessage() {
        return message.get();
    }

    public UserService() {
        this.em = JPAUtil.getEntityManager();
        this.userRepository = new UserRepository(em);
    }

    // Nouveau constructeur pour les tests permettant l'injection d'un UserRepository mocké
    public UserService(UserRepository userRepository) {
        // Pour les tests, on peut passer un EntityManager null ou un mock si nécessaire.
        this.em = null;
        this.userRepository = userRepository;
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public void logout() {
        this.currentUsername = null;
    }

    public String getCurrentUsername() {
        return currentUsername != null ? currentUsername : "Guest";
    }

    public User getCurrentUser() {
        return userRepository.getUserByUsername(currentUsername);
    }

    /**
     * Handles complete registration process including validation
     * @return true if registration succeeded
     */
    public boolean registerUser(String userName, String password, String confirmPassword) {
        if (!validateUsername(userName) || !validatePassword(password, confirmPassword)) {
            return false;
        }
        try {
            User newUser = createNewUser(userName, password);
            saveUserToDatabase(newUser);
            message.set("User registered successfully!");
            return true;
        }catch (RepositoryException e) {
            handleRegistrationFailure(e, "Registration failed: " + e.getMessage());
        } catch (Exception e) {
            handleRegistrationFailure(e, "An error occurred during registration");
        }
        return false;
    }

    private User createNewUser(String userName, String password) {
        String hashedPassword = hashPassword(password);
        return new User(userName.toUpperCase(), hashedPassword);
    }

    private void saveUserToDatabase(User user) throws RepositoryException {
        if (em != null) {
            em.getTransaction().begin();
        }
        userRepository.save(user);
        if (em != null) {
            em.getTransaction().commit();
        }
    }
    private void handleRegistrationFailure(Exception e, String errorMessage) {
        if (em != null && em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        message.set(errorMessage);
    }

    private boolean validatePassword(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            message.set("Passwords do not match!");
            return false;
        }

        if (password.length() < 7) {
            message.set("Password must be at least 7 characters long");
            return false;
        }
        return true;
    }

    private boolean validateUsername(String username) {
        if (username.length() < 3){
            message.set("Username must be at least 3 characters long");
            return false;
        }
        if (userRepository.existsByUserName(username)){
            message.set("Username already taken");
            return false;
        }
        return true;
    }

    /**
     * Authenticates a user by comparing the provided raw password with the stored hashed password.
     *
     * @param userName the username of the user to authenticate.
     * @param rawPassword the raw password provided by the user during login.
     * @throws RepositoryException if no user is found with the given username.
     * @return {@code true} if the username exists and the password matches, {@code false} otherwise.
     */
    public boolean authenticateUser(String userName, String rawPassword) {
        try {
            User user = userRepository.getUserByUsername(userName.toUpperCase());
            boolean authenticated = user.getPassword().equals(hashPassword(rawPassword));
            if (authenticated) {
                this.currentUsername = userName.toUpperCase();
            }
            return authenticated;
        } catch (RepositoryException e) {
            return false;
        }
    }

    /**
     * Hashes the provided raw password using SHA-256.
     *
     * @param rawPassword the password to hash.
     * @return the hashed password as a hexadecimal string.
     */
    String hashPassword(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawPassword.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}

