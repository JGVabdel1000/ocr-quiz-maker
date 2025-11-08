package be.esi.prj.model.services;

import static org.junit.jupiter.api.Assertions.*;

import be.esi.prj.model.orm.User;
import be.esi.prj.model.repository.UserRepository;
import be.esi.prj.model.repository.RepositoryException;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private UserService userService;
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        // Créer un mock pour UserRepository
        userRepository = mock(UserRepository.class);

        // Initialiser UserService avec le mock via le constructeur d'injection
        userService = new UserService(userRepository);
    }

    @Test
    public void testRegisterUser_success() {
        String username = "testuser";
        String password = "password123";
        String confirmPassword = "password123";

        // Simuler que le nom d'utilisateur n'existe pas
        when(userRepository.existsByUserName(username)).thenReturn(false);
        // Simuler la sauvegarde de l'utilisateur
        doNothing().when(userRepository).save(any(User.class));

        boolean result = userService.registerUser(username, password, confirmPassword);

        assertTrue(result);
        assertEquals("User registered successfully!", userService.getMessage());
    }

    @Test
    public void testRegisterUser_usernameTaken() {
        String username = "testuser";
        String password = "password123";
        String confirmPassword = "password123";

        // Simuler que le nom d'utilisateur existe déjà
        when(userRepository.existsByUserName(username)).thenReturn(true);

        boolean result = userService.registerUser(username, password, confirmPassword);

        assertFalse(result);
        assertEquals("Username already taken", userService.getMessage());
    }

    @Test
    public void testRegisterUser_passwordMismatch() {
        String username = "testuser";
        String password = "password123";
        String confirmPassword = "password321";

        boolean result = userService.registerUser(username, password, confirmPassword);

        assertFalse(result);
        assertEquals("Passwords do not match!", userService.getMessage());
    }

    @Test
    public void testRegisterUser_passwordTooShort() {
        String username = "testuser";
        String password = "pass";
        String confirmPassword = "pass";

        boolean result = userService.registerUser(username, password, confirmPassword);

        assertFalse(result);
        assertEquals("Password must be at least 7 characters long", userService.getMessage());
    }

    @Test
    public void testAuthenticateUser_success() throws RepositoryException {
        String username = "testuser";
        String password = "password123";

        // Créer un utilisateur mocké avec le mot de passe déjà haché
        User user = new User(username, userService.hashPassword(password));
        when(userRepository.getUserByUsername(username.toUpperCase())).thenReturn(user);

        boolean result = userService.authenticateUser(username, password);

        assertTrue(result);
        assertEquals(username.toUpperCase(), userService.getCurrentUsername());
    }


    @Test
    public void testAuthenticateUser_invalidPassword() throws RepositoryException {
        String username = "testuser";
        String password = "password123";

        // Créer un utilisateur mocké avec un mot de passe différent
        User user = new User(username, "wrongpassword");
        when(userRepository.getUserByUsername(username.toUpperCase())).thenReturn(user);

        boolean result = userService.authenticateUser(username, password);

        assertFalse(result);
    }

    @Test
    public void testAuthenticateUser_userNotFound() throws RepositoryException {
        String username = "nonexistentuser";
        String password = "password123";

        when(userRepository.getUserByUsername(username.toUpperCase()))
                .thenThrow(new RepositoryException("User not found"));

        boolean result = userService.authenticateUser(username, password);

        assertFalse(result);
    }

    @Test
    public void testHashPassword() {
        String password = "password123";
        String hashedPassword = userService.hashPassword(password);

        assertNotNull(hashedPassword);
        assertNotEquals(password, hashedPassword);
    }

    @Test
    public void testLogout() {
        String username = "testuser";
        String password = "password123";
        // Stubbing : renvoyer un utilisateur dont le mot de passe est déjà haché
        when(userRepository.getUserByUsername(username.toUpperCase()))
                .thenReturn(new User(username, userService.hashPassword(password)));

        // Authentifier l'utilisateur
        boolean authResult = userService.authenticateUser(username, password);
        assertTrue(authResult);
        assertEquals(username.toUpperCase(), userService.getCurrentUsername());

        // Déconnexion
        userService.logout();
        assertEquals("Guest", userService.getCurrentUsername());
    }


    @Test
    public void testRegisterUser_saveThrowsException() {
        String username = "testuser";
        String password = "password123";

        when(userRepository.existsByUserName(username)).thenReturn(false);
        doThrow(new RepositoryException("Database error"))
                .when(userRepository).save(any(User.class));

        boolean result = userService.registerUser(username, password, password);

        assertFalse(result);
        assertEquals("Registration failed: Database error", userService.getMessage());
    }
}


