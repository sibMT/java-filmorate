package ru.yandex.practicum.filmorate.storage.mpa;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.List;
import java.util.Optional;

@Repository
public class MpaDbStorage implements MpaStorage {
    private final JdbcTemplate jdbcTemplate;

    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MpaRating> getAllMpaRatings() {
        String sql = "SELECT * FROM mpa_ratings";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new MpaRating(
                        rs.getInt("mpa_id"),
                        rs.getString("name"),
                        rs.getString("code")
                )
        );
    }

    @Override
    public Optional<MpaRating> getMpaById(int id) {
        String sql = "SELECT * FROM mpa_ratings WHERE mpa_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                            new MpaRating(
                                    rs.getInt("mpa_id"),
                                    rs.getString("name"),
                                    rs.getString("code")
                            ),
                    id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
