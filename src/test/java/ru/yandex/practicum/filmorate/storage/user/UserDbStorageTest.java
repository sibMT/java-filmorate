package ru.yandex.practicum.filmorate.storage.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureTestDatabase
@Sql(scripts = {"/schema.sql", "/test-data.sql"})
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserDbStorageTest {

    @Autowired
    private UserDbStorage userStorage;

    @Test
    void getAllUsers_shouldReturn2InitialUsers() {
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
    void addUser_shouldAddUserWithGeneratedId() {
        User newUser = User.builder()
                .email("new@example.com")
                .login("newlogin")
                .name("New User")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = userStorage.addUser(newUser);

        assertThat(addedUser.getId()).isNotNull();
        assertThat(addedUser.getEmail()).isEqualTo("new@example.com");
        assertThat(userStorage.getAllUsers()).hasSize(3);
    }

    @Test
    void updateUser_shouldUpdateExistingUser() {
        User userToUpdate = userStorage.getUserById(1L);
        userToUpdate.setName("Updated Name");
        userToUpdate.setEmail("updated@example.com");

        User updatedUser = userStorage.updateUser(userToUpdate);

        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
        assertThat(userStorage.getUserById(1L).getEmail())
                .isEqualTo("updated@example.com");
    }

    @Test
    void getUserById_shouldReturnCorrectUser() {
        User user = userStorage.getUserById(2L);

        assertThat(user.getLogin()).isEqualTo("login2");
        assertThat(user.getName()).isEqualTo("User Two");
    }

    @Test
    void deleteUser_shouldRemoveUserFromDatabase() {
        userStorage.deleteUser(1L);

        assertThat(userStorage.getAllUsers()).hasSize(1);
        assertThrows(NotFoundException.class, () -> userStorage.getUserById(1L));
    }

    @Test
    void addFriend_shouldCreateFriendship() {
        userStorage.addFriend(1L, 2L);

        List<User> friends = userStorage.getFriends(1L);
        assertThat(friends).isEmpty(); // Дружба не подтверждена

        Set<Long> friendIds = userStorage.getFriendIds(1L);
        assertThat(friendIds).containsExactly(2L);
    }

    @Test
    void confirmFriendship_shouldUpdateStatus() {
        userStorage.addFriend(1L, 2L);
        userStorage.addFriend(2L, 1L);
        userStorage.confirmFriendship(1L, 2L);

        List<User> friends = userStorage.getFriends(1L);
        assertThat(friends)
                .hasSize(1)
                .extracting(User::getId)
                .containsExactly(2L);
    }

    @Test
    void removeFriend_shouldDeleteFriendship() {
        userStorage.addFriend(1L, 2L);
        userStorage.confirmFriendship(1L, 2L);
        userStorage.removeFriend(1L, 2L);

        assertThat(userStorage.getFriends(1L)).isEmpty();
    }

    @Test
    void getFriends_shouldReturnConfirmedFriends() {
        userStorage.addFriend(1L, 2L);
        userStorage.addFriend(2L, 1L);
        userStorage.confirmFriendship(1L, 2L);

        List<User> friends = userStorage.getFriends(1L);
        assertThat(friends)
                .hasSize(1)
                .extracting(User::getLogin)
                .containsExactly("login2");
    }

    @Test
    void getCommonFriends_shouldReturnMutualFriends() {
        User user3 = userStorage.addUser(
                User.builder()
                        .email("user3@example.com")
                        .login("user3")
                        .birthday(LocalDate.of(2000, 1, 1))
                        .build()
        );

        userStorage.addFriend(1L, user3.getId());
        userStorage.addFriend(2L, user3.getId());
        userStorage.confirmFriendship(1L, user3.getId());
        userStorage.confirmFriendship(2L, user3.getId());

        List<User> commonFriends = userStorage.getCommonFriends(1L, 2L);

        assertThat(commonFriends)
                .hasSize(1)
                .extracting(User::getId)
                .containsExactly(user3.getId());
    }

    @Test
    void getUserById_shouldThrowWhenUserNotFound() {
        assertThrows(NotFoundException.class, () -> userStorage.getUserById(999L));
    }

    @Test
    void updateUser_shouldThrowWhenUserNotFound() {
        User unknownUser = User.builder()
                .id(999L)
                .email("unknown@example.com")
                .login("unknown")
                .birthday(LocalDate.now())
                .build();

        assertThrows(NotFoundException.class, () -> userStorage.updateUser(unknownUser));
    }
}
