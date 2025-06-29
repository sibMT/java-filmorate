package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UserStorage {
    Collection<User> getAllUsers();

    User addUser(User user);

    User getUserById(Long id) throws NotFoundException;

    User updateUser(User user);

    void deleteUser(Long id);

    void addFriend(Long userId, Long friendId);

    void removeFriend(Long userId, Long friendId);

    List<User> getFriends(Long userId);

    Set<Long> getFriendIds(Long userId);

    List<User> getCommonFriends(Long userId1, Long userId2);

    boolean friendshipExists(Long userId, Long friendId);

    void confirmFriendship(Long userId, Long friendId);

    boolean userExists(Long userId);
}
