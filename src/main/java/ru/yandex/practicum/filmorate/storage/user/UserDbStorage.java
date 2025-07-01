package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Repository
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
        if (!userExists(user.getId())) {
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
            return jdbcTemplate.queryForObject(sql, this::mapRowToUser, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Пользователь с ID " + id + " не найден.");
        }
    }

    @Override
    public Collection<User> getAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, this::mapRowToUser);
    }

    @Override
    public void deleteUser(Long id) {
        jdbcTemplate.update("DELETE FROM friends WHERE user_id = ? OR friend_id = ?", id, id);
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", id);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {

        if (!userExists(userId) || !userExists(friendId)) {
            throw new NotFoundException("User not found");
        }

        String sql = "MERGE INTO friends KEY(user_id, friend_id) VALUES (?, ?)";

        jdbcTemplate.update(sql, userId, friendId);
        jdbcTemplate.update(sql, friendId, userId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        jdbcTemplate.update("DELETE FROM friends WHERE user_id = ? AND friend_id = ?", userId, friendId);
        jdbcTemplate.update("DELETE FROM friends WHERE user_id = ? AND friend_id = ?", friendId, userId);
    }


    @Override
    public List<User> getFriends(Long userId) {
        String sql = """
                SELECT u.user_id, u.email, u.login, u.name, u.birthday
                FROM friends f
                JOIN users u ON u.user_id = f.friend_id
                WHERE f.user_id = ?""";

        return jdbcTemplate.query(sql, this::mapRowToUser, userId);
    }

    @Override
    public List<User> getCommonFriends(Long userId1, Long userId2) {
        String sql = """
                SELECT u.*
                FROM friends f1
                JOIN friends f2 ON f1.friend_id = f2.friend_id
                JOIN users u ON f1.friend_id = u.user_id
                WHERE f1.user_id = ? AND f2.user_id = ?""";

        return jdbcTemplate.query(sql, this::mapRowToUser, userId1, userId2);
    }

    @Override
    public boolean userExists(Long userId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM users WHERE user_id = ?)";
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(sql, Boolean.class, userId)
        );
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        return User.builder()
                .id(rs.getLong("user_id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday").toLocalDate())
                .build();
    }

    public boolean hasFriend(Long userId, Long friendId) {
        String sql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, friendId);
        return count != null && count > 0;
    }
}
