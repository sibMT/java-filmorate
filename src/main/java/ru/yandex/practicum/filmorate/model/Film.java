package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Film {
    private Long id;

    @NotBlank(message = "Название не может быть пустым")
    private String name;

    @Size(max = 200, message = "Описание не должно превышать 200 символов")
    private String description;

    @NotNull(message = "Дата релиза должна быть указана")
    @PastOrPresent(message = "Дата релиза не может быть в будущем")
    private LocalDate releaseDate;

    @NotNull(message = "Продолжительность должна быть указана")
    @Positive(message = "Продолжительность должна быть положительной")
    private Integer duration;

    @Builder.Default
    private Set<Long> likes = new HashSet<>();

    private MpaRating mpa;
    private Set<Genre> genres = new HashSet<>();

    public Set<Integer> getGenreIds() {
        return genres.stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
    }

    @JsonSetter("likes")
    public void setLikes(Set<Long> likes) {
        this.likes = likes != null ? likes : new HashSet<>();
    }

    private int likesCount;

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

}
