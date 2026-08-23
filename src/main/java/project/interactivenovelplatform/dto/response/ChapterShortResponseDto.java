package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.interactivenovelplatform.entity.ChapterStatus;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChapterShortResponseDto {
    private Long id;
    private Double chapterNumber;
    private String title;
    private ChapterStatus status;
    private OffsetDateTime publishedAt;
}
