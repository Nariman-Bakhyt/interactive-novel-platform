package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.interactivenovelplatform.entity.ChapterStatus;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChapterResponseDto {
    private Long id;
    private Double chapterNumber;
    private String title;
    private List<ChapterBlockResponseDto> blocks;
    private ChapterStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime publishedAt;
}
