DELETE FROM likes;
DELETE FROM friends;
DELETE FROM film_genres;
DELETE FROM films;
DELETE FROM users;
DELETE FROM genres;
DELETE FROM mpa_ratings;

MERGE INTO mpa_ratings (mpa_id, name, code) KEY (mpa_id) VALUES
(1, 'G', 'G'),
(2, 'PG', 'PG'),
(3, 'PG-13', 'PG-13'),
(4, 'R', 'R'),
(5, 'NC-17', 'NC-17');

MERGE INTO genres (genre_id, name) KEY (genre_id) VALUES
(1, 'Комедия'),
(2, 'Драма'),
(3, 'Мультфильм'),
(4, 'Триллер'),
(5, 'Документальный'),
(6, 'Боевик');

INSERT INTO users (email, login, name, birthday) VALUES
('user1@example.com', 'login1', 'User One', '1990-01-01'),
('user2@example.com', 'login2', 'User Two', '1995-05-05');

INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES
('Film 1', 'Description 1', '2000-01-01', 120, 1),
('Film 2', 'Description 2', '2005-05-05', 90, 2);