package ru.yandex.practicum.filmorate.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.FilmNotFoundException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    public Collection<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public void deleteFilm(Long id) {
        filmStorage.deleteFilm(id);
    }

    public Film getFilmById(Long id) {
        return filmStorage.getFilmById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + id + " не найден"));
    }

    public Film addFilm(Film film) {
        if (film.getMpa() == null) {
            throw new ValidationException("MPA rating must be specified");
        }
        validateFilm(film);
        return filmStorage.addFilm(film);
    }

    public Film updateFilm(Film film) {
        Film existingFilm = getExistingFilm(film.getId());
        validateFilm(film);
        updateFilmFields(film, existingFilm);

        if (film.getMpa() != null) {
            existingFilm.setMpa(film.getMpa());
        }

        if (film.getGenres() != null) {
            existingFilm.setGenres(new HashSet<>(film.getGenres()));
        }

        return filmStorage.updateFilm(existingFilm);
    }

    public void addLike(Long filmId, Long userId) {
        if (!filmStorage.filmExists(filmId)) {
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }

        if (!userStorage.userExists(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        Set<Long> existingLikes = filmStorage.getFilmLikes(filmId);
        if (existingLikes.contains(userId)) {
            throw new ValidationException("Пользователь уже поставил лайк этому фильму");
        }

        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        if (!filmStorage.filmExists(filmId)) {
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }

        if (!userStorage.userExists(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.getPopularFilms(count);
    }

    private Film getExistingFilm(Long id) {
        return filmStorage.getFilmById(id)
                .orElseThrow(() -> new FilmNotFoundException("Фильм не найден"));
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            throw new ValidationException("Дата релиза не может быть раньше " + CINEMA_BIRTHDAY);
        }

        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность должна быть положительной");
        }
    }

    private void updateFilmFields(Film source, Film target) {
        Optional.ofNullable(source.getName()).ifPresent(target::setName);
        Optional.ofNullable(source.getDescription()).ifPresent(target::setDescription);
        Optional.ofNullable(source.getReleaseDate()).ifPresent(target::setReleaseDate);
        Optional.ofNullable(source.getDuration()).ifPresent(target::setDuration);
        Optional.ofNullable(source.getMpa()).ifPresent(target::setMpa);
    }

    public List<Genre> getAllGenres() {
        return genreStorage.getAllGenres();
    }

    public List<MpaRating> getAllMpaRatings() {
        return mpaStorage.getAllMpaRatings();
    }
}

