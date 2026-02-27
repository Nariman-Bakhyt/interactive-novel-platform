package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
public class RatingResponseDto {
    private Long ratingId;
    private Long totalScore;
    private Integer ratingCount;
    private Double averageRating;
    private String content;
    private String username;
    private OffsetDateTime timestamp;
    private Integer score;
}
