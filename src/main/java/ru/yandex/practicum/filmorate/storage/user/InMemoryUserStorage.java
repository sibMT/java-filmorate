package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private long nextInt = 1;

    @Override
    public Collection<User> getAllUsers() {
        return users.values();
    }

    @Override
    public User addUser(User user) {
        user.setId(genNextId());
        user.setName(noName(user));
        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }

        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User getUserById(Long id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь", id);
        }
        return user;
    }

    @Override
    public User updateUser(User user) {
        if (!users.containsKey(user.getId())) {
            String errorMessage = String.format("User with id %d not found", user.getId());
            log.error(errorMessage);
            throw new UserNotFoundException(errorMessage);
        }
        user.setName(getValidName(user));
        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>()); // Инициализация при обновлении
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        users.remove(id);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId); // Двусторонняя дружба
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        User user = getUserById(userId);
        User friend = getUserById(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId); // Удаление с обеих сторон
    }

    @Override
    public List<User> getFriends(Long userId) {
        User user = getUserById(userId);
        return user.getFriends().stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Long> getFriendIds(Long userId) {
        return new HashSet<>(getUserById(userId).getFriends());
    }

    @Override
    public List<User> getCommonFriends(Long userId1, Long userId2) {
        Set<Long> friends1 = getUserById(userId1).getFriends();
        Set<Long> friends2 = getUserById(userId2).getFriends();

        return friends1.stream()
                .filter(friends2::contains)
                .map(this::getUserById)
                .collect(Collectors.toList());
    }


    private long genNextId() {
        if (nextInt == Long.MAX_VALUE) {
            throw new IllegalStateException("Достигнут максимум ID пользователей");
        }
        return nextInt++;
    }

    private String noName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            return user.getLogin();
        } else {
            return user.getName();
        }
    }

    private String getValidName(User user) {
        return (user.getName() == null || user.getName().isBlank()) ? user.getLogin() : user.getName();
    }

}
