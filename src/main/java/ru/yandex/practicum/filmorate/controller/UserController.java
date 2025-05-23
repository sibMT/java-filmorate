package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
@Validated
public class UserController {

    private final Map<Long, User> users = new HashMap<>();
    private long nextInt = 1;

    @GetMapping
    public Collection<User> getAllUsers() {
        log.info("Запрос списка всех пользователей. Текущее количество: {}", users.size());
        return users.values();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public User addUser(@Valid @RequestBody User user) {
        log.debug("Попытка добавить пользователя: {}", user.getEmail());

        validateUserRules(user);
        validateUnique(user);

        user.setId(genNextId());
        user.setName(noName(user));
        users.put(user.getId(), user);

        log.info("Пользователь добавлен успешно. ID: {}, Имя: {}", user.getId(), user.getName());
        return user;
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User user) {
        log.debug("Попытка обновления пользователя: {}", user);

        if (user.getId() == null) {
            log.error("ID пользователя не указан");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id должен быть указан");
        }

        User existingUser = users.get(user.getId());
        if (existingUser == null) {
            log.error("Пользователь с ID {} не найден", user.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }

        validateUserRules(user);
        validateUpdateUnique(user, existingUser);
        updateUserFields(user, existingUser);

        log.info("Пользователь обновлён. ID: {}", user.getId());
        return existingUser;
    }

    private long genNextId() {
        if (nextInt == Long.MAX_VALUE) {
            throw new IllegalStateException("Достигнут максимум ID пользователей");
        }
        return nextInt++;
    }

    private void validateUnique(User user) {
        users.values().stream()
                .filter(existing -> !existing.getId().equals(user.getId()))
                .forEach(existing -> {
                    if (existing.getEmail().equalsIgnoreCase(user.getEmail())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email уже используется");
                    }
                    if (existing.getLogin().equalsIgnoreCase(user.getLogin())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Логин уже используется");
                    }
                });
    }

    private void validateUpdateUnique(User user, User existingUser) {
        if (!user.getEmail().equalsIgnoreCase(existingUser.getEmail())) {
            validateUnique(user);
        }
        if (!user.getLogin().equalsIgnoreCase(existingUser.getLogin())) {
            validateUnique(user);
        }
    }

    private void validateUserRules(User user) {
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дата рождения не может быть в будущем");
        }
    }

    private void updateUserFields(User user, User newUser) {
        newUser.setName(noName(user));
        newUser.setEmail(user.getEmail());
        newUser.setLogin(user.getLogin());
        newUser.setBirthday(user.getBirthday());
    }

    private String noName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            return user.getLogin();
        } else {
            return user.getName();
        }
    }
}
