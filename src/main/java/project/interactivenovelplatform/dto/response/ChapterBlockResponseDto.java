package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.interactivenovelplatform.entity.ChapterContentType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChapterBlockResponseDto {
    private Long id;
    private Integer sequenceOrder;
    private ChapterContentType type;
    private String content;
}