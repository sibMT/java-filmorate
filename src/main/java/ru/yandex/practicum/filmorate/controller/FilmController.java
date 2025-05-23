package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
@Validated
public class FilmController {

    private final Map<Long, Film> films = new HashMap<>();
    private long nextInt = 1;
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public Film addFilm(@Valid @RequestBody Film film) {
        log.debug("Добавление фильма: {}", film.getName());

        validateFilmRules(film);

        film.setId(genNextId());
        films.put(film.getId(), film);
        log.info("Фильм добавлен успешно. ID: {}, Название: {}", film.getId(), film.getName());
        return film;
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film film) {
        log.debug("Попытка обновления фильма ID: {}", film.getId());
        if (film.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id должен быть указан");
        }

        Film existingFilm = films.get(film.getId());
        if (existingFilm == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден");
        }

        validateFilmRules(film);
        updateFilmFields(film, existingFilm);
        log.info("Фильм обновлён. ID: {}", existingFilm.getId());
        return existingFilm;
    }

    @GetMapping
    public Collection<Film> getAllFilms() {
        log.info("Запрос списка всех фильмов. Текущее количество: {}", films.size());
        return films.values();
    }

    private void validateFilmRules(Film film) {
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

    private long genNextId() {
        return nextInt++;
    }

}
