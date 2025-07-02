package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Genre {
    private int id;
    private String name;

    public Genre(int id) {
        this.id = id;
        this.name = null;
    }
}
