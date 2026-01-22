package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import project.interactivenovelplatform.entity.Novel;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class NovelSearchRequestDto {
    private String title;
    private Long authorId;
    private List<Long> genreIds;
    private List<Long> tagIds;
    private BigDecimal minRating;
    private BigDecimal maxRating;
    private Novel status;
}
