package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FilmStorage {

    Optional<Film> getFilmById(Long id);

    Film addFilm(Film film);

    Film updateFilm(Film film);

    void deleteFilm(Long id);

    Collection<Film> getAllFilms();

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    List<Film> getPopularFilms(int count);

    boolean filmExists(Long filmId);
}
