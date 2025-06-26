# java-filmorate
Template repository for Filmorate project.


# Database Schema

![Database Diagram](schema.png)

## Описание схемы
Схема базы данных Filmorate включает 7 таблиц:
- `users` - хранит данные пользователей
- `films` - информация о фильмах
- `mpa_ratings` - справочник возрастных рейтингов
- `genres` - справочник жанров
- `film_genres` - связь фильмов и жанров (многие-ко-многим)
- `friends` - связи дружбы между пользователями
- `likes` - лайки фильмов пользователями

## Примеры запросов

### 1. Получение топ-5 популярных фильмов
```sql
SELECT f.name, COUNT(l.user_id) AS likes_count
FROM films f
LEFT JOIN likes l ON f.film_id = l.film_id
GROUP BY f.film_id
ORDER BY likes_count DESC
LIMIT 5;
```

### 2. Поиск общих друзей
```sql
SELECT u.name 
FROM friends f1
JOIN friends f2 ON f1.friend_id = f2.friend_id
JOIN users u ON f1.friend_id = u.user_id
WHERE f1.user_id = 1 AND f2.user_id = 2;
```

### 3. Получение фильмов по жанру
```sql
SELECT f.name 
FROM films f
JOIN film_genres fg ON f.film_id = fg.film_id
JOIN genres g ON fg.genre_id = g.genre_id
WHERE g.name = 'Комедия';
```

### 4. Обновление статуса дружбы
```sql
UPDATE friends
SET status = TRUE
WHERE user_id = 1 AND friend_id = 2;
```

### 5. Получение всех данных о фильме
```sql
SELECT f.*, m.name AS mpa, GROUP_CONCAT(g.name) AS genres
FROM films f
JOIN mpa_ratings m ON f.mpa_id = m.mpa_id
LEFT JOIN film_genres fg ON f.film_id = fg.film_id
LEFT JOIN genres g ON fg.genre_id = g.genre_id
WHERE f.film_id = 1
GROUP BY f.film_id;
```