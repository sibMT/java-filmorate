package ru.yandex.practicum.filmorate.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {
    private HttpStatus status;
    private String message;
    private String details;
    private LocalDateTime timestamp;
    @Builder.Default
    private List<String> subErrors = new ArrayList<>();

    public void addSubError(String error) {
        this.subErrors.add(error);
    }
}
