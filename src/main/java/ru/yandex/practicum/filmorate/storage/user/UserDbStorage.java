package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mappers.UserRowMapper;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Slf4j
@Repository
@Qualifier("userDbStorage")
@Primary
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("users")
                .usingGeneratedKeyColumns("user_id");
    }

    @Override
    public User addUser(User user) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("email", user.getEmail());
        parameters.put("login", user.getLogin());
        parameters.put("name", user.getName());
        parameters.put("birthday", user.getBirthday());

        Number newId = jdbcInsert.executeAndReturnKey(parameters);
        user.setId(newId.longValue());
        return user;
    }

    @Override
    public User updateUser(User user) {
        String checkSql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, user.getId());

        if (count == null || count == 0) {
            throw new NotFoundException("User with id " + user.getId() + " not found");
        }

        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId());
        return user;
    }

    @Override
    public User getUserById(Long id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, new UserRowMapper(), id);
            if (user != null) {
                user.setFriends(getFriendIds(id));
            }
            return user;
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Пользователь с ID " + id + " не найден.");
        }
    }

    @Override
    public Collection<User> getAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }

    @Override
    public void deleteUser(Long id) {
        jdbcTemplate.update("DELETE FROM friends WHERE user_id = ? OR friend_id = ?", id, id);
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", id);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        if (userNotExists(userId)) throw new NotFoundException("User " + userId + " not found");
        if (userNotExists(friendId)) throw new NotFoundException("User " + friendId + " not found");

        if (!friendshipExists(userId, friendId)) {
            String sql = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, false)";
            jdbcTemplate.update(sql, userId, friendId);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        jdbcTemplate.update(sql, userId, friendId, friendId, userId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        String sql = "SELECT u.* FROM users u "
                + "JOIN friends f ON u.user_id = f.friend_id "
                + "WHERE f.user_id = ?";
        return jdbcTemplate.query(sql, new UserRowMapper(), userId);
    }

    @Override
    public Set<Long> getFriendIds(Long userId) {
        String sql = "SELECT friend_id FROM friends WHERE user_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, userId));
    }

    @Override
    public List<User> getCommonFriends(Long userId1, Long userId2) {
        log.debug("Поиск общих друзей для {} и {}", userId1, userId2);

        String sql = """
                SELECT u.*
                FROM friends f1
                JOIN friends f2 ON f1.friend_id = f2.friend_id
                JOIN users u ON f1.friend_id = u.user_id
                WHERE f1.user_id = ? AND f2.user_id = ?
                """;

        List<User> friends = jdbcTemplate.query(sql, new UserRowMapper(), userId1, userId2);
        log.debug("Найдено общих друзей: {}", friends.size());
        return friends;
    }

    public boolean userExists(Long userId) {
        String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }

    public boolean friendshipExists(Long userId, Long friendId) {
        String sql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, friendId);
        return count != null && count > 0;
    }

    @Override
    public void confirmFriendship(Long userId, Long friendId) {
        String sql = "UPDATE friends SET status = true "
                + "WHERE (user_id = ? AND friend_id = ?) "
                + "OR (user_id = ? AND friend_id = ?)";
        jdbcTemplate.update(sql, userId, friendId, friendId, userId);
    }

    private boolean userNotExists(Long userId) {
        return !userExists(userId);
    }

    private void removeFriendship(Long user1, Long user2) {
        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, user1, user2);
        if (rowsDeleted > 0) {
            log.info("Удалена дружба: {} -> {}", user1, user2);
        }
    }

    @Override
    public boolean friendshipRequestExists(Long userId, Long friendId) {
        String sql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, friendId);
        return count != null && count > 0;
    }
}
