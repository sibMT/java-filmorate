package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MpaRating {
    private int id;
    private String name;
    private String code;

    public MpaRating(int id) {
        this.id = id;
        this.name = null;
        this.code = null;
    }
}
