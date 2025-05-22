package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Long, User> users = new HashMap<>();
    private long nextInt = 1;

    @GetMapping
    public Collection<User> getAllUsers() {
        log.info("Запрос списка всех пользователей. Текущее количество: {}", users.size());
        if (users.isEmpty()) {
            log.warn("Список пользователей пуст");
        }
        return users.values();
    }

    @PostMapping
    public User addUser(@RequestBody User user) {
        log.debug("Попытка добавить пользователя: {}", user);
        try {
            validateUserFields(user);
            validateUnique(user);

            if (user.getName() == null || user.getName().isBlank()) {
                user.setName(user.getLogin());
            }

            user.setId(genNextId());
            users.put(user.getId(), user);

            log.info("Пользователь добавлен успешно. ID: {}, Имя: {}", user.getId(), user.getName());
            return user;
        } catch (ConditionsNotMetException e) {
            log.warn("Ошибка валидации при добавлении: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (DuplicatedDataException e) {
            log.warn("Конфликт данных при добавлении: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        log.debug("Попытка обновления пользователя: {}", user);
        try {
            if (user.getId() == null) {
                log.error("ID пользователя не указан");
                throw new ConditionsNotMetException("Id должен быть указан");
            }

            User existingUser = users.get(user.getId());
            if (existingUser == null) {
                log.error("Пользователь с ID {} не найден", user.getId());
                throw new ConditionsNotMetException("Пользователь не найден");
            }

            validateUserFields(user);
            validateUpdateUnique(user, existingUser);

            if (user.getName() == null || user.getName().isBlank()) {
                user.setName(user.getLogin());
            }

            users.put(user.getId(), user);
            log.info("Пользователь обновлён. ID: {}", user.getId());
            return user;
        } catch (ConditionsNotMetException e) {
            log.warn("Ошибка валидации при обновлении: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (DuplicatedDataException e) {
            log.warn("Конфликт данных при обновлении: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    private long genNextId() {
        if (nextInt == Long.MAX_VALUE) {
            throw new IllegalStateException("Достигнут максимум ID пользователей");
        }
        return nextInt++;
    }

    private void validateUserFields(User user) {
        log.debug("Валидация пользователя: {}", user);

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.error("Email не может быть пустым");
            throw new ConditionsNotMetException("Email не может быть пустым");
        }

        if (!user.getEmail().contains("@")) {
            log.error("Некорректный email: {}", user.getEmail());
            throw new ConditionsNotMetException("Email должен содержать @");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.error("Логин не может быть пустым");
            throw new ConditionsNotMetException("Логин не может быть пустым");
        }

        if (user.getLogin().contains(" ")) {
            log.error("Логин содержит пробелы: {}", user.getLogin());
            throw new ConditionsNotMetException("Логин не может содержать пробелы");
        }

        if (user.getBirthday() == null) {
            log.error("Дата рождения не указана");
            throw new ConditionsNotMetException("Дата рождения не может быть пустой");
        }

        if (user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Некорректная дата рождения: {}", user.getBirthday());
            throw new ConditionsNotMetException("Дата рождения не может быть в будущем");
        }

        log.debug("Валидация пройдена: {}", user);
    }

    private void validateUnique(User user) {
        users.values().stream()
                .filter(u -> user.getEmail().equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .ifPresent(u -> {
                    log.warn("Попытка использовать существующий email: {}", user.getEmail());
                    throw new DuplicatedDataException("Email уже используется");
                });

        users.values().stream()
                .filter(u -> user.getLogin().equalsIgnoreCase(u.getLogin()))
                .findFirst()
                .ifPresent(u -> {
                    log.warn("Попытка использовать существующий логин: {}", user.getLogin());
                    throw new DuplicatedDataException("Логин уже используется");
                });
    }

    private void validateUpdateUnique(User user, User existingUser) {
        if (!user.getEmail().equalsIgnoreCase(existingUser.getEmail())) {
            users.values().stream()
                    .filter(u -> !u.getId().equals(user.getId()))
                    .filter(u -> user.getEmail().equalsIgnoreCase(u.getEmail()))
                    .findFirst()
                    .ifPresent(u -> {
                        log.warn("Конфликт email при обновлении: {}", user.getEmail());
                        throw new DuplicatedDataException("Email уже используется");
                    });
        }

        if (!user.getLogin().equalsIgnoreCase(existingUser.getLogin())) {
            users.values().stream()
                    .filter(u -> !u.getId().equals(user.getId()))
                    .filter(u -> user.getLogin().equalsIgnoreCase(u.getLogin()))
                    .findFirst()
                    .ifPresent(u -> {
                        log.warn("Конфликт логина при обновлении: {}", user.getLogin());
                        throw new DuplicatedDataException("Логин уже используется");
                    });
        }
    }
}
