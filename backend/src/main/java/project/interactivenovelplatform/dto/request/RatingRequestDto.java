package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RatingRequestDto {
    @NotNull(message = "Оценка не может быть пустой")
    @Min(1)
    @Max(5)
    private Integer score;
    private String commentText;
}
