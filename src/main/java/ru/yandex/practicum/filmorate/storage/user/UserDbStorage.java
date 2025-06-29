package ru.yandex.practicum.filmorate.storage.user;

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
        if (!userExists(userId)) throw new NotFoundException("User " + userId + " not found");
        if (!userExists(friendId)) throw new NotFoundException("User " + friendId + " not found");
        String sql = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, true), (?, ?, true)";
        jdbcTemplate.update(sql, userId, friendId, friendId, userId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friends f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ? AND f.status = true";
        return jdbcTemplate.query(sql, new UserRowMapper(), userId);
    }

    @Override
    public Set<Long> getFriendIds(Long userId) {
        String sql = "SELECT friend_id FROM friends WHERE user_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, userId));
    }

    @Override
    public List<User> getCommonFriends(Long userId1, Long userId2) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friends f1 ON u.user_id = f1.friend_id " +
                "JOIN friends f2 ON u.user_id = f2.friend_id " +
                "WHERE f1.user_id = ? AND f2.user_id = ? " +
                "AND u.user_id NOT IN (?, ?)";
        return jdbcTemplate.query(sql, new UserRowMapper(), userId1, userId2, userId1, userId2);
    }

    private boolean userExists(Long userId) {
        String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }
}
