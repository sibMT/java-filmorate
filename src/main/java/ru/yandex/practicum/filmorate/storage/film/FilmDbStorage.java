package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.*;

@Repository
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbc;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public Film addFilm(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) " +
                "VALUES (:name, :description, :releaseDate, :duration, :mpaId)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", film.getName())
                .addValue("description", film.getDescription())
                .addValue("releaseDate", film.getReleaseDate())
                .addValue("duration", film.getDuration())
                .addValue("mpaId", film.getMpa().getId());

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        namedJdbc.update(sql, params, keyHolder);
        film.setId(keyHolder.getKeyAs(Long.class));

        updateFilmGenres(film);
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE film_id = ?";
        int updated = jdbcTemplate.update(sql, film.getName(), film.getDescription(), film.getReleaseDate(),
                film.getDuration(), film.getMpa().getId(), film.getId());

        if (updated == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        updateFilmGenres(film);
        return film;
    }


    private void updateFilmGenres(Film film) {
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            List<Object[]> batchArgs = film.getGenres().stream()
                    .map(genre -> new Object[]{film.getId(), genre.getId()})
                    .toList();
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        String sql = "SELECT f.*, m.name AS mpa_name, m.code AS mpa_code, " +
                "(SELECT COUNT(*) FROM likes l WHERE l.film_id = f.film_id) AS likes_count " +
                "FROM films f JOIN mpa_ratings m ON f.mpa_id = m.mpa_id WHERE f.film_id = ?";

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), id);
        if (films.isEmpty()) return Optional.empty();

        Film film = films.get(0);
        enrichGenres(List.of(film));
        enrichLikes(List.of(film));
        return Optional.of(film);
    }

    @Override
    public Collection<Film> getAllFilms() {
        String sql = "SELECT f.*, m.name AS mpa_name, m.code AS mpa_code, " +
                "(SELECT COUNT(*) FROM likes l WHERE l.film_id = f.film_id) AS likes_count " +
                "FROM films f JOIN mpa_ratings m ON f.mpa_id = m.mpa_id";

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper());
        enrichGenres(films);
        enrichLikes(films);
        return films;
    }

    private void enrichGenres(List<Film> films) {
        if (films.isEmpty()) return;
        Map<Long, Set<Genre>> filmGenres = new HashMap<>();
        String sql = "SELECT fg.film_id, g.genre_id, g.name FROM film_genres fg JOIN genres g ON " +
                "fg.genre_id = g.genre_id WHERE fg.film_id IN (:ids)";

        List<Long> ids = films.stream().map(Film::getId).toList();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);

        namedJdbc.query(sql, params, rs -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = new Genre(rs.getInt("genre_id"), rs.getString("name"));
            filmGenres.computeIfAbsent(filmId, k -> new HashSet<>()).add(genre);
        });

        for (Film film : films) {
            film.setGenres(filmGenres.getOrDefault(film.getId(), new HashSet<>()));
        }
    }

    private void enrichLikes(List<Film> films) {
        if (films.isEmpty()) return;
        Map<Long, Set<Long>> filmLikes = new HashMap<>();
        String sql = "SELECT film_id, user_id FROM likes WHERE film_id IN (:ids)";

        List<Long> ids = films.stream().map(Film::getId).toList();
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);

        namedJdbc.query(sql, params, rs -> {
            Long filmId = rs.getLong("film_id");
            Long userId = rs.getLong("user_id");
            filmLikes.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        });

        for (Film film : films) {
            film.setLikes(filmLikes.getOrDefault(film.getId(), new HashSet<>()));
        }
    }

    @Override
    public void deleteFilm(Long id) {
        jdbcTemplate.update("DELETE FROM films WHERE film_id = ?", id);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        jdbcTemplate.update("DELETE FROM likes WHERE film_id = ? AND user_id = ?", filmId, userId);
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbcTemplate.update("MERGE INTO likes (film_id, user_id) KEY(film_id, user_id) " +
                "VALUES (?, ?)", filmId, userId);
    }

    @Override
    public boolean filmExists(Long filmId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM films WHERE film_id = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, filmId));
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, m.name AS mpa_name, m.code AS mpa_code, COUNT(l.user_id) AS likes_count " +
                "FROM films f LEFT JOIN likes l ON f.film_id = l.film_id " +
                "JOIN mpa_ratings m ON f.mpa_id = m.mpa_id " +
                "GROUP BY f.film_id ORDER BY likes_count DESC LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), count);
        enrichGenres(films);
        enrichLikes(films);
        return films;
    }

    @Override
    public Set<Long> getFilmLikes(Long filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, filmId));
    }
}
