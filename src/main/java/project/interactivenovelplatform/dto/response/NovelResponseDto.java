package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
public class NovelResponseDto {
    Long id;
    String title;
    String status;
    String description;
    OffsetDateTime publicationDate;
    int chapterCount;
    BigDecimal averageRating;
    int ratingCount;
    Long viewCount;
    String authorName;
    String coverUrl;

}
