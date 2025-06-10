package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;


public interface UserStorage {
    Collection<User> getAllUsers();

    User addUser(User user);

    User getUserById(Long id);

    User updateUser(User user);

    void deleteUser(Long id);
}
