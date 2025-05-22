package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmorateApplicationTests {

    protected final FilmController filmController = new FilmController();
    protected final UserController userController = new UserController();
    protected User user;
    protected User invalidUser;
    protected Film film;
    protected Film invalidFilm;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("ivan@email.com");
        user.setLogin("ivanLogin");
        user.setName("Ivan");
        user.setBirthday(LocalDate.of(1997, 3, 5));

        invalidUser = new User();
        invalidUser.setEmail("maxim-email.com");
        invalidUser.setLogin("maxim login");
        invalidUser.setBirthday(LocalDate.now().plusDays(1));

        film = new Film();
        film.setName("Transformers");
        film.setDescription("Роботы-машины");
        film.setDate(LocalDate.of(2007, 3, 10));
        film.setDuration(95);

        invalidFilm = new Film();
        invalidFilm.setName("");
        invalidFilm.setDescription("!".repeat(201));
        invalidFilm.setDate(LocalDate.of(1900, 1, 1));
        invalidFilm.setDuration(-100);
    }

    @AfterEach
    void tearDown() {
        userController.getAllUsers().clear();
        filmController.getAllFilms().clear();
    }

    @Test
    void contextLoads() {
        assertNotNull(filmController, "FilmController загружен некорректно");
        assertNotNull(userController, "UserController загружен некорректно");
    }

    @Test
    void createUser() {
        User created = userController.addUser(user);
        assertNotNull(created.getId());
        assertEquals("ivan@email.com", created.getEmail());
    }

    @Test
    void rejectUserWithInvalidEmail() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userController.addUser(invalidUser));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Email должен содержать @"));
    }

    @Test
    void rejectUserWithSpacesInLogin() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userController.addUser(invalidUser));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void createFilm() {
        Film created = filmController.addFilm(film);
        assertEquals("Transformers", created.getName());
    }

    @Test
    void rejectFilmWithEmptyName() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> filmController.addFilm(invalidFilm));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Name не может быть пустым"));
    }

    @Test
    void rejectFilmWithNegativeDuration() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> filmController.addFilm(invalidFilm));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateUserSuccessfully() {
        User newUser = userController.addUser(user);

        User updateData = new User();
        updateData.setId(newUser.getId());
        updateData.setEmail("new@email.com");
        updateData.setLogin("newLogin");
        updateData.setBirthday(user.getBirthday());

        User updated = userController.updateUser(updateData);

        assertEquals(newUser.getId(), updated.getId());
        assertEquals("new@email.com", updated.getEmail());
        assertEquals("newLogin", updated.getLogin());
        assertEquals("newLogin", updated.getName());
        assertEquals(user.getBirthday(), updated.getBirthday());
    }

    @Test
    void rejectUpdateUserWithInvalidData() {
        User created = userController.addUser(user);

        User invalidUpdate = new User();
        invalidUpdate.setId(created.getId());
        invalidUpdate.setLogin("invalid login");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userController.updateUser(invalidUpdate));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void rejectUpdateForNonExistentUser() {
        User nonExistentUpdate = new User();
        nonExistentUpdate.setId(500L);
        nonExistentUpdate.setLogin("newLogin");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userController.updateUser(nonExistentUpdate));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updateFilmSuccessfully() {
        Film created = filmController.addFilm(film);

        Film updateData = new Film();
        updateData.setId(created.getId());
        updateData.setName("Новое имя");
        updateData.setDuration(200);
        updateData.setDescription("Новое описание");
        updateData.setDate(LocalDate.of(2000, 10, 30));

        Film updated = filmController.updateFilm(updateData);

        assertEquals(created.getId(), updated.getId());
        assertEquals("Новое имя", updated.getName());
        assertEquals(200, updated.getDuration());
        assertEquals("Новое описание", updated.getDescription());
        assertEquals(LocalDate.of(2000, 10, 30), updated.getDate());
    }

    @Test
    void rejectUpdateFilmWithInvalidName() {
        Film createdFilm = filmController.addFilm(film);

        Film invalidUpdate = new Film();
        invalidUpdate.setId(createdFilm.getId());
        invalidUpdate.setName(" ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> filmController.updateFilm(invalidUpdate));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void rejectPartialUpdateWithInvalidData() {
        Film created = filmController.addFilm(film);

        Film partialUpdate = new Film();
        partialUpdate.setId(created.getId());
        partialUpdate.setDuration(-10);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> filmController.updateFilm(partialUpdate));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void notChangeFieldsDuringUpdate() {
        Film original = filmController.addFilm(film);

        Film update = new Film();
        update.setId(original.getId());
        update.setName("Новое название");

        Film updated = filmController.updateFilm(update);

        assertEquals(film.getDescription(), updated.getDescription());
        assertEquals(film.getDuration(), updated.getDuration());
        assertEquals(film.getDate(), updated.getDate());
    }

    @Test
    void rejectDuplicateEmailWhenUpdating() {
        User user1 = userController.addUser(user);
        User user2 = new User();
        user2.setName("Anton");
        user2.setEmail("anton@email.com");
        user2.setLogin("antonLogin");
        user2.setBirthday(LocalDate.of(2005, 11, 7));
        user2 = userController.addUser(user2);

        User updatedUser = new User();
        updatedUser.setId(user2.getId());
        updatedUser.setName("Tony");
        updatedUser.setBirthday(LocalDate.of(1970, 5, 5));
        updatedUser.setLogin("tonyLogin");
        updatedUser.setEmail(user1.getEmail());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userController.updateUser(updatedUser));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}