package ru.yandex.practicum.filmorate.exception;

public class FilmNotFoundException extends NotFoundException {
    public FilmNotFoundException(String message) {
        super(message);
    }
}
