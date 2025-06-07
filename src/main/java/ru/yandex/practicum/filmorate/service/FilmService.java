package ru.yandex.practicum.filmorate.service;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);


    public Collection<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public Film addFilm(Film film) {
        validateFilm(film);
        return filmStorage.addFilm(film);
    }

    public Film updateFilm(Film film) {
        Film existingFilm = filmStorage.getFilmById(film.getId());
        validateFilm(existingFilm);
        updateFilmFields(film, existingFilm);
        return filmStorage.updateFilm(existingFilm);
    }

    public Film getFilmById(Long id) {
        return filmStorage.getFilmById(id);
    }

    public void deleteFilm(Long id) {
        filmStorage.deleteFilm(id);
    }

    public void addLike(Long filmId, Long userId) {
        Film film = filmStorage.getFilmById(filmId);
        User user = userStorage.getUserById(userId);

        if (film.getLikes().contains(userId)) {
            throw new ValidationException("Пользователь уже лайкнул");
        }

        film.getLikes().add(userId);
        filmStorage.updateFilm(film);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.getAllFilms().stream()
                .sorted(Comparator.comparingInt(f -> -f.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
    }

    private void validateFilm(Film film) {
        log.debug("Проверка правил для фильма {}", film);
        if (film.getDate().isBefore(CINEMA_BIRTHDAY)) {
            log.error("Дата релиза {} раньше дня рождения кино ({})", film.getDate(), CINEMA_BIRTHDAY);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата релиза не может быть раньше " + CINEMA_BIRTHDAY);
        }
        if (film.getDuration() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Продолжительность должна быть положительным числом");
        }
        if (film.getDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дата релиза должна быть указана");
        }

        log.debug("Правила проверены успешно");
    }

    private void updateFilmFields(Film film, Film existingFilm) {
        if (film.getName() != null) {
            existingFilm.setName(film.getName());
        }
        if (film.getDescription() != null) {
            existingFilm.setDescription(film.getDescription());
        }
        if (film.getDate() != null) {
            existingFilm.setDate(film.getDate());
        }
        if (film.getDuration() != null) {
            existingFilm.setDuration(film.getDuration());
        }

    }
}
