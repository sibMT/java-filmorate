package ru.yandex.practicum.filmorate.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;

    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public User addUser(User user) {
        validateUser(user);
        validateUnique(user);
        return userStorage.addUser(user);
    }

    public User getUserById(Long id) {
        return userStorage.getUserById(id);
    }

    public User updateUser(User user) {
        User existingUser = userStorage.getUserById(user.getId());
        validateUpdateUnique(user, existingUser);
        updateUserFields(user, existingUser);
        validateUser(existingUser);
        return userStorage.updateUser(existingUser);
    }

    public void deleteUser(Long id) {
        userStorage.deleteUser(id);
    }

    public void addFriend(Long userId, Long friendId) {
        User user = userStorage.getUserById(userId);
        User friend = userStorage.getUserById(friendId);

        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }
        if (friend.getFriends() == null) {
            friend.setFriends(new HashSet<>());
        }

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        userStorage.updateUser(user);
        userStorage.updateUser(friend);;
    }

    public List<User> getFriends(Long userId) {
        User user = userStorage.getUserById(userId);
        if (user.getFriends() == null) {
            return List.of();
        }
        return user.getFriends().stream()
                .map(userStorage::getUserById)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(Long userId1, Long userId2) {
        User user1 = userStorage.getUserById(userId1);
        User user2 = userStorage.getUserById(userId2);

        Set<Long> friends1 = user1.getFriends() != null ? user1.getFriends() : Set.of();
        Set<Long> friends2 = user2.getFriends() != null ? user2.getFriends() : Set.of();

        return friends1.stream()
                .filter(friends2::contains)
                .map(userStorage::getUserById)
                .collect(Collectors.toList());
    }

    public void removeFriend(Long userId, Long friendId) {
        User user = userStorage.getUserById(userId);
        User friend = userStorage.getUserById(friendId);

        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }
        if (friend.getFriends() == null) {
            friend.setFriends(new HashSet<>());
        }

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        userStorage.updateUser(user);
        userStorage.updateUser(friend);
    }

    private void validateUser(User user) {
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Birthday не может быть в будущем");
        }
    }

    private void updateUserFields(User source, User target) {
        target.setName(getValidName(source));
        target.setEmail(source.getEmail());
        target.setLogin(source.getLogin());
        target.setBirthday(source.getBirthday());
    }

    private String getValidName(User user) {
        return (user.getName() == null || user.getName().isBlank()) ? user.getLogin() : user.getName();
    }

    private void validateUnique(User user) {
        userStorage.getAllUsers().stream()
                .filter(existing -> !existing.getId().equals(user.getId()))
                .forEach(existing -> {
                    if (existing.getEmail().equalsIgnoreCase(user.getEmail())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email уже используется");
                    }
                    if (existing.getLogin().equalsIgnoreCase(user.getLogin())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Логин уже используется");
                    }
                });
    }

    private void validateUpdateUnique(User user, User existingUser) {
        if (!user.getEmail().equalsIgnoreCase(existingUser.getEmail())) {
            validateUnique(user);
        }
        if (!user.getLogin().equalsIgnoreCase(existingUser.getLogin())) {
            validateUnique(user);
        }
    }
}
