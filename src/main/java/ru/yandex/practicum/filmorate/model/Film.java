package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Film {
    private Long id;
    private String name;
    private String description;
    @JsonProperty("releaseDate")
    private LocalDate date;
    private Integer duration;
}
