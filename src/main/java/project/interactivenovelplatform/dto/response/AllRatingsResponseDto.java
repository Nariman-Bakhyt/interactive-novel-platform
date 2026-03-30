package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;

@Getter
@Setter
@AllArgsConstructor
public class AllRatingsResponseDto {
    private Long totalScore;
    private Integer ratingCount;
    private Double averageRating;
    private PagedModel<AllRatingResponseDto> allRatings;
}
