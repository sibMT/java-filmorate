package ru.yandex.practicum.filmorate.storage.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private UserDbStorage userStorage;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        userStorage = new UserDbStorage(jdbcTemplate);

        user1 = userStorage.addUser(createTestUser("Max@mail.com", "user1", "Max"));
        user2 = userStorage.addUser(createTestUser("Ivan@mail.com", "user2", "Ivan"));
        user3 = userStorage.addUser(createTestUser("Andrey@mail.com", "user3", "Andrey"));
    }

    private User createTestUser(String email, String login, String name) {
        return User.builder()
                .email(email)
                .login(login)
                .name(name)
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void addUser() {
        User newUser = createTestUser("new@mail.com", "newlogin", "New User");

        User addedUser = userStorage.addUser(newUser);

        assertThat(addedUser.getId()).isNotNull();
        assertThat(addedUser.getEmail()).isEqualTo(newUser.getEmail());
        assertThat(addedUser.getLogin()).isEqualTo(newUser.getLogin());
    }

    @Test
    void getUserById() {
        User retrievedUser = userStorage.getUserById(user1.getId());

        assertThat(retrievedUser)
                .usingRecursiveComparison()
                .isEqualTo(user1);
    }

    @Test
    void getUserById_() {
        assertThrows(NotFoundException.class, () -> userStorage.getUserById(999L));
    }

    @Test
    void updateUser() {
        User updatedUser = User.builder()
                .id(user1.getId())
                .email("updated@mail.com")
                .login("updatedlogin")
                .name("Updated Name")
                .birthday(LocalDate.of(2000, 1, 1))
                .build();

        User result = userStorage.updateUser(updatedUser);

        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(updatedUser);

        User retrievedUser = userStorage.getUserById(user1.getId());
        assertThat(retrievedUser)
                .usingRecursiveComparison()
                .isEqualTo(updatedUser);
    }

    @Test
    void getAllUsers() {
        Collection<User> users = userStorage.getAllUsers();

        assertThat(users)
                .hasSize(3)
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder(
                        user1.getEmail(),
                        user2.getEmail(),
                        user3.getEmail()
                );
    }

    @Test
    void deleteUser() {
        userStorage.addFriend(user1.getId(), user2.getId());
        userStorage.addFriend(user2.getId(), user1.getId());
        userStorage.confirmFriendship(user1.getId(), user2.getId());

        userStorage.deleteUser(user1.getId());

        assertThrows(NotFoundException.class, () -> userStorage.getUserById(user1.getId()));

        assertThat(userStorage.getFriends(user2.getId())).isEmpty();
    }

    @Test
    void addFriend() {
        userStorage.addFriend(user1.getId(), user2.getId());

        Set<Long> friendIds = userStorage.getFriendIds(user1.getId());
        assertThat(friendIds).containsExactly(user2.getId());

        assertThat(userStorage.getFriends(user1.getId())).isEmpty();
    }

    @Test
    void confirmFriendship() {
        userStorage.addFriend(user1.getId(), user2.getId());
        userStorage.addFriend(user2.getId(), user1.getId());

        userStorage.confirmFriendship(user1.getId(), user2.getId());

        assertThat(userStorage.getFriends(user1.getId()))
                .extracting(User::getId)
                .containsExactly(user2.getId());

        assertThat(userStorage.getFriends(user2.getId()))
                .extracting(User::getId)
                .containsExactly(user1.getId());
    }

    @Test
    void removeFriend() {
        userStorage.addFriend(user1.getId(), user2.getId());
        userStorage.addFriend(user2.getId(), user1.getId());
        userStorage.confirmFriendship(user1.getId(), user2.getId());

        userStorage.removeFriend(user1.getId(), user2.getId());

        assertThat(userStorage.getFriends(user1.getId())).isEmpty();
        assertThat(userStorage.getFriends(user2.getId())).isEmpty();
    }

    @Test
    void getCommonFriends() {
        userStorage.addFriend(user1.getId(), user3.getId());
        userStorage.addFriend(user2.getId(), user3.getId());

        userStorage.addFriend(user3.getId(), user1.getId());
        userStorage.addFriend(user3.getId(), user2.getId());
        userStorage.confirmFriendship(user1.getId(), user3.getId());
        userStorage.confirmFriendship(user2.getId(), user3.getId());

        List<User> commonFriends = userStorage.getCommonFriends(user1.getId(), user2.getId());

        assertThat(commonFriends)
                .hasSize(1)
                .extracting(User::getId)
                .containsExactly(user3.getId());
    }

    @Test
    void getFriends() {
        userStorage.addFriend(user1.getId(), user2.getId());

        userStorage.addFriend(user1.getId(), user3.getId());
        userStorage.addFriend(user3.getId(), user1.getId());
        userStorage.confirmFriendship(user1.getId(), user3.getId());

        List<User> friends = userStorage.getFriends(user1.getId());

        assertThat(friends)
                .hasSize(1)
                .extracting(User::getId)
                .containsExactly(user3.getId());
    }

    @Test
    void getFriendIds() {
        userStorage.addFriend(user1.getId(), user2.getId());

        userStorage.addFriend(user1.getId(), user3.getId());
        userStorage.addFriend(user3.getId(), user1.getId());
        userStorage.confirmFriendship(user1.getId(), user3.getId());

        Set<Long> friendIds = userStorage.getFriendIds(user1.getId());

        assertThat(friendIds)
                .hasSize(2)
                .containsExactlyInAnyOrder(user2.getId(), user3.getId());
    }
}
