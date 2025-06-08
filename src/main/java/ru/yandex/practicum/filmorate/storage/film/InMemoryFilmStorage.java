package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.FilmNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private long nextId = 1;

    @Override
    public Collection<Film> getAllFilms() {
        return films.values();
    }

    @Override
    public Film addFilm(Film film) {
        if (film.getLikes() == null) {
            film.setLikes(new HashSet<>());
        }
        film.setId(generateNextId());
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        if (!films.containsKey(film.getId())) {
            String errorMessage = String.format("Film with id %d not found", film.getId());
            log.error(errorMessage);
            throw new FilmNotFoundException(errorMessage);
        }
        films.put(film.getId(), film);
        log.info("Film updated. ID: {}", film.getId());
        return film;
    }

    @Override
    public Film getFilmById(Long id) {
        if (!films.containsKey(id)) {
            String errorMessage = String.format("Film with id %d not found", id);
            log.error(errorMessage);
            throw new FilmNotFoundException(errorMessage);
        }
        return films.get(id);
    }

    @Override
    public void deleteFilm(Long id) {
        films.remove(id);
    }

    private long generateNextId() {
        return nextId++;
    }
}
