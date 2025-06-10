package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.Optional;

public interface FilmStorage {

    Optional<Film> getFilmById(Long id);

    Film addFilm(Film film);

    Film updateFilm(Film film);

    void deleteFilm(Long id);

    Collection<Film> getAllFilms();
}
