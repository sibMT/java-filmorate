package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping
    public Collection<User> getAllUsers() {
        log.info("Запрос списка всех пользователей.");
        return userService.getAllUsers();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public User addUser(@Valid @RequestBody User user) {
        log.debug("Попытка добавить пользователя: {}", user.getEmail());
        log.info("Пользователь добавлен успешно. ID: {}, Имя: {}", user.getId(), user.getName());
        return userService.addUser(user);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        log.info("Получен Http запрос на получение пользователя по id: {}", id);
        log.debug("Найден пользователь: {}", userService.getUserById(id));
        return userService.getUserById(id);
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User user) {
        log.debug("Попытка обновления пользователя: {}", user);
        log.info("Пользователь обновлён. ID: {}", user.getId());
        return userService.updateUser(user);
    }

    @DeleteMapping
    public void deleteUser(@PathVariable Long id) {
        log.info("Запрос на удаление пользователя с id {}", id);
        userService.deleteUser(id);
    }

    @PutMapping("/{id}/friends/{friendsId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        userService.addFriend(id, friendId);
    }

    @GetMapping("/{id}/friends")
    public List<User> getFriends(@PathVariable Long id) {
        return userService.getFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable Long id, @PathVariable Long otherId) {
        return userService.getCommonFriends(id, otherId);
    }
}
