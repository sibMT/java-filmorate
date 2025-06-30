INSERT INTO mpa_ratings (mpa_id, name, code) VALUES
(1, 'G', 'G'),
(2, 'PG', 'PG'),
(3, 'PG-13', 'PG-13'),
(4, 'R', 'R'),
(5, 'NC-17', 'NC-17');

INSERT INTO genres (genre_id, name)
 SELECT * FROM (VALUES
(1, 'Комедия'),
(2, 'Драма'),
(3, 'Мультфильм'),
(4, 'Триллер'),
(5, 'Документальный'),
(6, 'Боевик')
)AS new_genres(genre_id,name)
WHERE NOT EXISTS (
SELECT 1 FROM genres
WHERE genre_id = new_genres.genres_id)


INSERT INTO users (email, login, name, birthday) VALUES
('Ivan1@example.com', 'login1', 'Ivan', '2000-01-01'),
('Max2@example.com', 'login2', 'Maxim', '1997-05-27');

INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES
('Матрица', 'Test description 1', '2000-01-01', 120, 1),
('Океан', 'Test description 2', '2010-05-15', 90, 3);