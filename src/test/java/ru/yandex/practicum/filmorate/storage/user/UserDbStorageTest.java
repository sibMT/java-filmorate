package ru.yandex.practicum.filmorate.storage.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = {"/schema.sql", "/test-data.sql"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserDbStorageTest {

    @Autowired
    private UserDbStorage userStorage;

    private User createTestUser(String email, String login, String name) {
        return User.builder()
                .email(email)
                .login(login)
                .name(name)
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void getAllUsers() {
        Collection<User> users = userStorage.getAllUsers();
        assertThat(users)
                .hasSize(2)
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder(
                        "user1@example.com",
                        "user2@example.com"
                );
    }

    @Test
    void addUser() {
        User newUser = createTestUser("new@example.com", "newlogin", "New User");
        User addedUser = userStorage.addUser(newUser);

        assertThat(addedUser.getId()).isNotNull();
        assertThat(addedUser.getEmail()).isEqualTo("new@example.com");
        assertThat(userStorage.getAllUsers()).hasSize(3);
    }

    @Test
    void updateUser() {
        User userToUpdate = userStorage.getUserById(1L);
        userToUpdate.setName("Updated Name");
        userToUpdate.setEmail("updated@example.com");

        User updatedUser = userStorage.updateUser(userToUpdate);

        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
        assertThat(userStorage.getUserById(1L).getEmail())
                .isEqualTo("updated@example.com");
    }

    @Test
    void getUserById() {
        User user = userStorage.getUserById(2L);
        assertThat(user.getLogin()).isEqualTo("login2");
        assertThat(user.getName()).isEqualTo("User Two");
    }

    @Test
    void deleteUser() {
        assertThat(userStorage.getAllUsers())
                .extracting(User::getId)
                .containsExactly(1L, 2L);

        userStorage.deleteUser(1L);

        assertThat(userStorage.getAllUsers())
                .hasSize(1)
                .extracting(User::getId)
                .containsExactly(2L);

        assertThrows(NotFoundException.class, () -> userStorage.getUserById(1L));
    }

    @Test
    void addFriend() {
        userStorage.addFriend(1L, 2L);

        List<User> user1Friends = userStorage.getFriends(1L);
        assertThat(user1Friends)
                .hasSize(1)
                .extracting(User::getId)
                .containsExactly(2L);

        List<User> user2Friends = userStorage.getFriends(2L);
        assertThat(user2Friends)
                .hasSize(1)
                .extracting(User::getId)
                .containsExactly(1L);
    }

//    @Test
//    void removeFriend() {
//        userStorage.addFriend(1L, 2L);
//
//        userStorage.removeFriend(1L, 2L);
//
//        assertThat(userStorage.getFriends(1L)).isEmpty();
//        assertThat(userStorage.getFriends(2L)).isEmpty();
//    }

    @Test
    void getFriends() {
        userStorage.addFriend(1L, 2L);

        List<User> friends = userStorage.getFriends(1L);
        assertThat(friends)
                .hasSize(1)
                .extracting(User::getLogin)
                .containsExactly("login2");
    }

    @Test
    void addFriendTwiceShouldNotDuplicate() {
        userStorage.addFriend(1L, 2L);
        userStorage.addFriend(1L, 2L);

        List<User> friends = userStorage.getFriends(1L);
        assertThat(friends).hasSize(1);
    }

    @Test
    void getCommonFriends() {
        User user3 = userStorage.addUser(
                createTestUser("user3@mail.com", "user3", "User Three")
        );

        userStorage.addFriend(1L, user3.getId());

        userStorage.addFriend(2L, user3.getId());

        List<User> commonFriends = userStorage.getCommonFriends(1L, 2L);
        assertThat(commonFriends)
                .hasSize(1)
                .extracting(User::getId)
                .containsExactly(user3.getId());
    }

    @Test
    void getUserById_shouldThrowWhenNotFound() {
        assertThrows(NotFoundException.class, () -> userStorage.getUserById(999L));
    }

    @Test
    void updateUser_shouldThrowWhenNotFound() {
        User unknownUser = createTestUser("unknown@example.com", "unknown", "Unknown");
        unknownUser.setId(999L);
        assertThrows(NotFoundException.class, () -> userStorage.updateUser(unknownUser));
    }

    @Test
    void addFriend_shouldThrowWhenUserNotFound() {
        assertThrows(NotFoundException.class, () -> userStorage.addFriend(1L, 999L));
    }

    @Test
    void removeFriend_shouldNotThrowWhenFriendshipNotExist() {
        assertDoesNotThrow(() -> userStorage.removeFriend(1L, 2L));
    }
}
