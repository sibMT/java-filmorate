package ru.yandex.practicum.filmorate.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.FilmNotFoundException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    @Qualifier("filmDbStorage")
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;


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
        if (film.getLikes() == null) {
            film.setLikes(new HashSet<>());
        }
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
        Film film = getExistingFilm(filmId);
        User user = userStorage.getUserById(userId);

        if (!film.getLikes().add(userId)) {
            throw new ValidationException("Пользователь уже поставил лайк этому фильму");
        }
        filmStorage.updateFilm(film);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = getExistingFilm(filmId);
        User user = userStorage.getUserById(userId);

        if (!film.getLikes().remove(userId)) {
            throw new ValidationException("Пользователь не ставил лайк этому фильму");
        }
        filmStorage.updateFilm(film);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.getAllFilms().stream()
                .filter(f -> f.getLikes() != null)
                .sorted(Comparator.comparingInt(f -> -f.getLikes().size()))
                .limit(Math.max(count, 1))
                .collect(Collectors.toList());
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
