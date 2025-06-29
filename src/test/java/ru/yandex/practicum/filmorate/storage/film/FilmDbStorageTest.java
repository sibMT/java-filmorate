package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@Sql(scripts = {"/schema.sql", "/test-data.sql"})
class FilmDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private FilmDbStorage filmStorage;

    @BeforeEach
    void setUp() {
        filmStorage = new FilmDbStorage(jdbcTemplate);
    }

    @Test
    void addFilm_shouldAddAndRetrieveFilm() {
        Film newFilm = Film.builder()
                .name("New Film")
                .description("New Description")
                .releaseDate(LocalDate.of(2023, 1, 1))
                .duration(120)
                .mpa(new MpaRating(1, "G", "G")) // Добавьте ожидаемые name и code
                .build();

        Film addedFilm = filmStorage.addFilm(newFilm);
        assertThat(addedFilm.getId()).isNotNull();

        Optional<Film> retrievedFilm = filmStorage.getFilmById(addedFilm.getId());
        assertThat(retrievedFilm)
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .ignoringFields("id", "genres", "likes")
                .isEqualTo(newFilm);
    }

    @Test
    void updateFilm_shouldUpdateExistingFilm() {
        Film existingFilm = filmStorage.getFilmById(1L).orElseThrow();
        existingFilm.setName("Updated Film");
        existingFilm.setDuration(150);

        Film updatedFilm = filmStorage.updateFilm(existingFilm);
        assertThat(updatedFilm.getName()).isEqualTo("Updated Film");
        assertThat(updatedFilm.getDuration()).isEqualTo(150);

        Optional<Film> retrievedFilm = filmStorage.getFilmById(1L);
        assertThat(retrievedFilm)
                .isPresent()
                .get()
                .satisfies(film -> {
                    assertThat(film.getName()).isEqualTo("Updated Film");
                    assertThat(film.getDuration()).isEqualTo(150);
                });
    }

    @Test
    void updateFilm_shouldThrowWhenFilmNotFound() {
        Film unknownFilm = Film.builder()
                .id(999L)
                .name("Unknown Film")
                .releaseDate(LocalDate.now())
                .duration(100)
                .mpa(new MpaRating(1))
                .build();

        assertThrows(NotFoundException.class, () -> filmStorage.updateFilm(unknownFilm));
    }

    @Test
    void getAllFilms_shouldReturnAllFilms() {
        Collection<Film> films = filmStorage.getAllFilms();
        assertThat(films)
                .hasSize(2)
                .extracting(Film::getName)
                .containsExactlyInAnyOrder("Film 1", "Film 2");
    }

    @Test
    void deleteFilm_shouldRemoveFilm() {
        filmStorage.deleteFilm(1L);

        Optional<Film> deletedFilm = filmStorage.getFilmById(1L);
        assertThat(deletedFilm).isEmpty();

        Collection<Film> remainingFilms = filmStorage.getAllFilms();
        assertThat(remainingFilms)
                .hasSize(1)
                .extracting(Film::getName)
                .containsExactly("Film 2");
    }

    @Test
    void addLike_shouldIncrementLikesCount() {
        filmStorage.addLike(1L, 1L);
        filmStorage.addLike(1L, 2L);

        Film film = filmStorage.getFilmById(1L).orElseThrow();
        assertThat(film.getLikes()).hasSize(2);
    }

    @Test
    void removeLike_shouldDecrementLikesCount() {
        filmStorage.addLike(1L, 1L);
        filmStorage.addLike(1L, 2L);
        filmStorage.removeLike(1L, 2L);

        Film film = filmStorage.getFilmById(1L).orElseThrow();
        assertThat(film.getLikes()).hasSize(1);
    }

    @Test
    void getPopularFilms_shouldReturnOrderedByLikes() {
        filmStorage.addLike(1L, 1L);
        filmStorage.addLike(1L, 2L);
        filmStorage.addLike(2L, 1L);

        List<Film> popularFilms = filmStorage.getPopularFilms(2);

        assertThat(popularFilms)
                .hasSize(2)
                .extracting(Film::getId)
                .containsExactly(1L, 2L); // Фильм 1 имеет 2 лайка, фильм 2 - 1 лайк
    }

}
