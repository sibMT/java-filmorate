package ru.yandex.practicum.filmorate.storage.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@Sql(scripts = {"/schema.sql", "/test-data.sql"})
class UserDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private UserDbStorage userStorage;

    @BeforeEach
    void setUp() {
        userStorage = new UserDbStorage(jdbcTemplate);
    }

    @Test
    void addUser_shouldAddAndRetrieveUser() {
        User newUser = User.builder()
                .email("test@example.com")
                .login("test_login")
                .name("Test User")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User addedUser = userStorage.addUser(newUser);
        assertThat(addedUser.getId()).isNotNull();

        User retrievedUser = userStorage.getUserById(addedUser.getId());
        assertThat(retrievedUser)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(newUser);
    }

    @Test
    void updateUser_shouldUpdateExistingUser() {
        User existingUser = userStorage.getUserById(1L);
        existingUser.setName("Updated Name");
        existingUser.setEmail("updated@example.com");

        User updatedUser = userStorage.updateUser(existingUser);
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");

        User retrievedUser = userStorage.getUserById(1L);
        assertThat(retrievedUser.getName()).isEqualTo("Updated Name");
        assertThat(retrievedUser.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void updateUser_shouldThrowWhenUserNotFound() {
        User unknownUser = User.builder()
                .id(999L)
                .email("unknown@test.com")
                .login("unknown")
                .birthday(LocalDate.now())
                .build();

        assertThrows(NotFoundException.class, () -> userStorage.updateUser(unknownUser));
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        Collection<User> users = userStorage.getAllUsers();
        assertThat(users)
                .hasSize(2)
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("user1@example.com", "user2@example.com");
    }

    @Test
    void deleteUser_shouldRemoveUser() {
        userStorage.deleteUser(1L);

        assertThrows(NotFoundException.class, () -> userStorage.getUserById(1L));

        Collection<User> remainingUsers = userStorage.getAllUsers();
        assertThat(remainingUsers)
                .hasSize(1)
                .extracting(User::getEmail)
                .containsExactly("user2@example.com");
    }

    @Test
    void addFriend_shouldAddFriendship() {
        userStorage.addFriend(1L, 2L);

        Set<Long> friends = userStorage.getFriendIds(1L);
        assertThat(friends).containsExactly(2L);
    }

    @Test
    void removeFriend_shouldRemoveFriendship() {
        userStorage.addFriend(1L, 2L);
        userStorage.removeFriend(1L, 2L);

        Set<Long> friends = userStorage.getFriendIds(1L);
        assertThat(friends).isEmpty();
    }

    @Test
    void getCommonFriends_shouldReturnMutualFriends() {
        User user3 = User.builder()
                .email("user3@example.com")
                .login("login3")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();
        userStorage.addUser(user3);

        userStorage.addFriend(1L, 2L);
        userStorage.addFriend(1L, user3.getId());
        userStorage.addFriend(2L, user3.getId());

        List<User> commonFriends = userStorage.getCommonFriends(1L, 2L);

        assertThat(commonFriends)
                .hasSize(1)
                .extracting(User::getId)
                .containsExactly(user3.getId());
    }

    @Test
    void getUserFriends_shouldReturnFriendsList() {
        userStorage.addFriend(1L, 2L);
        Set<Long> friends = userStorage.getFriendIds(1L);

        assertThat(friends)
                .hasSize(1)
                .containsExactly(2L);
    }

}
