package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter
@Setter
@AllArgsConstructor
public class AllRatingsResponseDto {
    private Long totalScore;
    private Integer ratingCount;
    private Double averageRating;
    private Page<AllRatingResponseDto> allRatings;
}
