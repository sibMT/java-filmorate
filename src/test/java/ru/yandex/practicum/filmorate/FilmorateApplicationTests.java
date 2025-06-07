package ru.yandex.practicum.filmorate;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmorateApplicationTests {


    @Autowired
    private FilmController filmController;

    @Autowired
    private UserController userController;

    @Autowired
    private FilmService filmService;

    @Autowired
    private UserService userService;

    protected User user;
    protected User invalidUser;
    protected Film film;
    protected Film invalidFilm;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .email("ivan@email.com")
                .login("ivanLogin")
                .name("Ivan")
                .birthday(LocalDate.of(1997, 10, 4))
                .build();

        invalidUser = User.builder()
                .email("maxim-email.com")
                .login("maxim login")
                .birthday(LocalDate.now().plusDays(1))
                .build();

        film = Film.builder()
                .name("Transformers")
                .description("Роботы-машины")
                .date(LocalDate.of(2007, 3, 10))
                .duration(95)
                .build();

        invalidFilm = Film.builder()
                .name("")
                .description("!".repeat(201))
                .date(LocalDate.of(1900, 1, 1))
                .duration(-100)
                .build();
    }

    @AfterEach
    void tearDown() {
        userController.getAllUsers().clear();
        filmController.getAllFilms().clear();
    }


    @Test
    void contextLoads() {
        assertNotNull(filmController);
        assertNotNull(userController);
        assertNotNull(filmService);
        assertNotNull(userService);
    }

    @Test
    void createUser() {
        User created = userController.addUser(user);
        assertNotNull(created.getId());
        assertEquals("ivan@email.com", created.getEmail());
    }

    @Test
    void rejectUserWithInvalidEmail() {
        assertThrows(ValidationException.class,
                () -> userController.addUser(invalidUser));
    }

    @Test
    void createFilm() {
        Film created = filmController.addFilm(film);
        assertEquals("Transformers", created.getName());
    }

    @Test
    void rejectFilmWithEmptyName() {
        assertThrows(ValidationException.class,
                () -> filmController.addFilm(invalidFilm));
    }

    @Test
    void updateUserSuccessfully() {
        User newUser = userController.addUser(user);

        User updateData = User.builder()
                .id(newUser.getId())
                .email("new@email.com")
                .login("newLogin")
                .birthday(user.getBirthday())
                .build();

        User updated = userController.updateUser(updateData);

        assertEquals(newUser.getId(), updated.getId());
        assertEquals("new@email.com", updated.getEmail());
        assertEquals("newLogin", updated.getLogin());
    }

    @Test
    void rejectUpdateForNonExistentUser() {
        User nonExistentUpdate = User.builder()
                .id(500L)
                .email("newEmail@email.com")
                .login("newLogin")
                .build();

        assertThrows(UserNotFoundException.class,
                () -> userController.updateUser(nonExistentUpdate));
    }

    @Test
    void updateFilmSuccessfully() {
        Film created = filmController.addFilm(film);

        Film updateData = Film.builder()
                .id(created.getId())
                .name("Новое имя")
                .duration(200)
                .description("Новое описание")
                .date(LocalDate.of(2000, 10, 30))
                .build();

        Film updated = filmController.updateFilm(updateData);

        assertEquals(created.getId(), updated.getId());
        assertEquals("Новое имя", updated.getName());
    }

    @Test
    void testFriends() {
        User user1 = userController.addUser(
                User.builder()
                        .email("user1@email.com")
                        .login("user1Login")
                        .name("User 1")
                        .birthday(LocalDate.of(1990, 1, 1))
                        .build()
        );

        User user2 = userController.addUser(
                User.builder()
                        .email("user2@email.com")
                        .login("user2Login")
                        .name("User 2")
                        .birthday(LocalDate.of(1995, 5, 5))
                        .build()
        );

        userController.addFriend(user1.getId(), user2.getId());

        List<User> user1Friends = userController.getFriends(user1.getId());
        assertEquals(1, user1Friends.size());
        assertEquals(user2.getId(), user1Friends.get(0).getId());

        List<User> user2Friends = userController.getFriends(user2.getId());
        assertEquals(1, user2Friends.size());
        assertEquals(user1.getId(), user2Friends.get(0).getId());

        User commonFriend = userController.addUser(
                User.builder()
                        .email("common@email.com")
                        .login("commonFriend")
                        .name("Common Friend")
                        .birthday(LocalDate.of(1985, 3, 15))
                        .build()
        );

        userController.addFriend(user1.getId(), commonFriend.getId());
        userController.addFriend(user2.getId(), commonFriend.getId());

        List<User> commonFriends = userController.getCommonFriends(user1.getId(), user2.getId());
        assertEquals(1, commonFriends.size());
        assertEquals(commonFriend.getId(), commonFriends.get(0).getId());
    }

    @Test
    void testFilmLikes() {
        Film film1 = filmController.addFilm(film);
        User user1 = userController.addUser(user);

        filmController.addLike(film1.getId(), user1.getId());

        List<Film> popular = filmController.getPopularFilms(1);
        assertEquals(1, popular.size());
        assertEquals(film1.getId(), popular.get(0).getId());
        assertEquals(1, popular.get(0).getLikes().size());
    }
}