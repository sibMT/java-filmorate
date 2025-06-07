package ru.yandex.practicum.filmorate.handlers;


import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.ApiError;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException unfe) {
        log.error("Not found exception error: {}", unfe.getMessage());
        return ApiError.builder()
                .errorCode(HttpStatus.NOT_FOUND.value())
                .description(unfe.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUncaught(Exception exception) {
        log.error("Internal server error: {}", exception.getMessage(), exception);
        return ApiError.builder()
                .errorCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .description(exception.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(ValidationException va) {
        log.error("Validation exception: {}", va.getMessage());
        return ApiError.builder()
                .errorCode(HttpStatus.BAD_REQUEST.value())
                .description(va.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.error("Validation exception: {}", errorMessage);
        return ApiError.builder()
                .errorCode(HttpStatus.BAD_REQUEST.value())
                .description(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

//    @RestControllerAdvice
//    public class GlobalExceptionHandler {
//
//        @ExceptionHandler(UserNotFoundException.class)
//        @ResponseStatus(HttpStatus.NOT_FOUND)
//        public ErrorResponse handleUserNotFound(UserNotFoundException ex) {
//            return new ErrorResponse(ex.getMessage());
//        }
//
//        @ExceptionHandler(FilmNotFoundException.class)
//        @ResponseStatus(HttpStatus.NOT_FOUND)
//        public ErrorResponse handleFilmNotFound(FilmNotFoundException ex) {
//            return new ErrorResponse(ex.getMessage());
//        }
//
//        @ExceptionHandler(ValidationException.class)
//        @ResponseStatus(HttpStatus.BAD_REQUEST)
//        public ErrorResponse handleValidation(ValidationException ex) {
//            return new ErrorResponse(ex.getMessage());
//        }
//    }

}
