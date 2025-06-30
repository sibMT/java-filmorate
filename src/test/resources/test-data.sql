DELETE FROM likes;
DELETE FROM friends;
DELETE FROM film_genres;
DELETE FROM films;
DELETE FROM users;
DELETE FROM genres;
DELETE FROM mpa_ratings;

ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1;
ALTER TABLE films ALTER COLUMN film_id RESTART WITH 1;

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

-- Добавляем 18 пользователей
INSERT INTO users (email, login, name, birthday) VALUES
('user1@example.com', 'login1', 'User One', '1990-01-01'),
('user2@example.com', 'login2', 'User Two', '1995-05-05'),
('user3@example.com', 'login3', 'User Three', '1990-01-01'),
('user4@example.com', 'login4', 'User Four', '1995-05-05'),
('user5@example.com', 'login5', 'User Five', '1990-01-01'),
('user6@example.com', 'login6', 'User Six', '1995-05-05'),
('user7@example.com', 'login7', 'User Seven', '1990-01-01'),
('user8@example.com', 'login8', 'User Eight', '1995-05-05'),
('user9@example.com', 'login9', 'User Nine', '1990-01-01'),
('user10@example.com', 'login10', 'User Ten', '1995-05-05'),
('user11@example.com', 'login11', 'User Eleven', '1990-01-01'),
('user12@example.com', 'login12', 'User Twelve', '1995-05-05'),
('user13@example.com', 'login13', 'User Thirteen', '1990-01-01'),
('user14@example.com', 'login14', 'User Fourteen', '1995-05-05'),
('user15@example.com', 'login15', 'User Fifteen', '1990-01-01'),
('user16@example.com', 'login16', 'User Sixteen', '1995-05-05'),
('user17@example.com', 'login17', 'User Seventeen', '1990-01-01'),
('user18@example.com', 'login18', 'User Eighteen', '1995-05-05');

INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES
('Film 1', 'Description 1', '2000-01-01', 120, 1),
('Film 2', 'Description 2', '2005-05-05', 90, 2);
