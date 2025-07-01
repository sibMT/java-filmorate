package ru.yandex.practicum.filmorate.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class UserRowMapper implements RowMapper<User> {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        Long userId = rs.getLong("user_id");

        Set<Long> friends = new HashSet<>(jdbcTemplate.queryForList(
                "SELECT friend_id FROM friends WHERE user_id = ?", Long.class, userId
        ));

        return User.builder()
                .id(userId)
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday").toLocalDate())
                .friends(friends)
                .build();
    }
}
