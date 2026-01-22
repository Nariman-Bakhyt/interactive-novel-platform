package project.interactivenovelplatform.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RatingRequestDto {
    private int score;
    private String commentText;
}
