package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RatingStatsDto {
    private Long totalScore;
    private Integer ratingCount;
    private Double averageRating;
}
