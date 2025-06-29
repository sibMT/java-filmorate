package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Sql(scripts = {"/schema.sql", "/test-data.sql"})
class GenreDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private GenreDbStorage genreStorage;

    @BeforeEach
    void setUp() {
        genreStorage = new GenreDbStorage(jdbcTemplate);
    }

    @Test
    void getAllGenres_shouldReturnAllGenres() {
        List<Genre> genres = genreStorage.getAllGenres();
        assertThat(genres)
                .hasSize(6)
                .extracting(Genre::getName)
                .contains("Комедия", "Драма", "Мультфильм");
    }

    @Test
    void getGenreById_shouldReturnCorrectGenre() {
        Optional<Genre> genre = genreStorage.getGenreById(2);
        assertThat(genre)
                .isPresent()
                .get()
                .satisfies(g -> assertThat(g.getName()).isEqualTo("Драма"));
    }
}
