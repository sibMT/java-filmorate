package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Sql(scripts = {"/schema.sql", "/test-data.sql"})
class MpaDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private MpaDbStorage mpaStorage;

    @BeforeEach
    void setUp() {
        mpaStorage = new MpaDbStorage(jdbcTemplate);
    }

    @Test
    void getAllMpa_shouldReturnAllRatings() {
        List<MpaRating> ratings = mpaStorage.getAllMpaRatings();
        assertThat(ratings)
                .hasSize(5)
                .extracting(MpaRating::getName)
                .contains("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    void getMpaById_shouldReturnCorrectRating() {
        Optional<MpaRating> rating = mpaStorage.getMpaById(3);
        assertThat(rating)
                .isPresent()
                .get()
                .satisfies(r -> assertThat(r.getName()).isEqualTo("PG-13"));
    }
}
