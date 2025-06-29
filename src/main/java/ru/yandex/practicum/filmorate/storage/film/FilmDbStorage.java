package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.*;

@Repository
@Qualifier("filmDbStorage")
@Primary
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("films")
                .usingGeneratedKeyColumns("film_id");
    }

    @Override
    public Film addFilm(Film film) {
        validateMpa(film);
        validateGenres(film);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", film.getName());
        parameters.put("description", film.getDescription());
        parameters.put("release_date", film.getReleaseDate());
        parameters.put("duration", film.getDuration());
        parameters.put("mpa_id", film.getMpa().getId());

        Number newId = jdbcInsert.executeAndReturnKey(parameters);
        film.setId(newId.longValue());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            jdbcTemplate.batchUpdate(sql,
                    film.getGenres().stream()
                            .map(genre -> new Object[]{film.getId(), genre.getId()})
                            .toList()
            );
        }

        return getFilmById(film.getId()).orElseThrow();
    }

    @Override
    public Film updateFilm(Film film) {
        String checkSql = "SELECT COUNT(*) FROM films WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, film.getId());

        if (count == null || count == 0) {
            throw new NotFoundException("Film with id " + film.getId() + " not found");
        }
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE film_id = ?";
        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        updateGenres(film);
        return getFilmById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм не найден после обновления"));
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        String sql = """
                SELECT
                    f.film_id,
                    f.name,
                    f.description,
                    f.release_date,
                    f.duration,
                    f.mpa_id,
                    m.name AS mpa_name,
                    m.code AS mpa_code
                FROM films f
                JOIN mpa_ratings m ON f.mpa_id = m.mpa_id
                WHERE f.film_id = ?
                """;

        try {
            Film film = jdbcTemplate.queryForObject(sql, new FilmRowMapper(), id);
            if (film != null) {
                film.setGenres(getFilmGenres(id));
                film.setLikes(getFilmLikes(id));
            }
            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Collection<Film> getAllFilms() {
        String sql = """
                SELECT f.*,
                       m.name AS mpa_name,
                       m.code AS mpa_code
                FROM films f
                JOIN mpa_ratings m ON f.mpa_id = m.mpa_id
                """;

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper());
        films.forEach(film -> {
            film.setGenres(getFilmGenres(film.getId()));
            film.setLikes(getFilmLikes(film.getId()));
        });
        return films;
    }

    @Override
    public void deleteFilm(Long id) {
        jdbcTemplate.update("DELETE FROM films WHERE film_id = ?", id);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbcTemplate.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        int deleted = jdbcTemplate.update(sql, filmId, userId);

        if (deleted == 0) {
            throw new NotFoundException("Like not found");
        }
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = """
                SELECT f.*, m.name AS mpa_name, m.code AS mpa_code
                FROM films f
                JOIN mpa_ratings m ON f.mpa_id = m.mpa_id
                LEFT JOIN likes l ON f.film_id = l.film_id
                GROUP BY f.film_id
                ORDER BY COUNT(l.user_id) DESC, f.film_id
                LIMIT ?
                """;

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), count);
        films.forEach(film -> {
            film.setGenres(getFilmGenres(film.getId()));
            film.setLikes(getFilmLikes(film.getId()));
        });
        return films;
    }

    private void updateGenres(Film film) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            for (Genre genre : film.getGenres()) {
                jdbcTemplate.update(sql, film.getId(), genre.getId());
            }
        }
    }

    private Set<Genre> getFilmGenres(Long filmId) {
        String sql = """
                SELECT
                    g.genre_id,
                    g.name
                FROM genres g
                JOIN film_genres fg ON g.genre_id = fg.genre_id
                WHERE fg.film_id = ?
                ORDER BY g.genre_id ASC
                """;

        List<Genre> genres = jdbcTemplate.query(sql,
                (rs, rowNum) -> new Genre(rs.getInt("genre_id"), rs.getString("name")),
                filmId
        );

        return new LinkedHashSet<>(genres);
    }

    private Set<Long> getFilmLikes(Long filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, filmId));
    }

    private void validateGenres(Film film) {
        if (film.getGenres() != null) {
            String sql = "SELECT COUNT(*) FROM genres WHERE genre_id = ?";
            for (Genre genre : film.getGenres()) {
                Integer count = jdbcTemplate.queryForObject(sql, Integer.class, genre.getId());
                if (count == null || count == 0) {
                    throw new NotFoundException("Genre not found: " + genre.getId());
                }
            }
        }
    }

    private void validateMpa(Film film) {
        if (film.getMpa() != null) {
            String sql = "SELECT COUNT(*) FROM mpa_ratings WHERE mpa_id = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, film.getMpa().getId());
            if (count == null || count == 0) {
                throw new NotFoundException("MPA rating not found");
            }
        }
    }
}
