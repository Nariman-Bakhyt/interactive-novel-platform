package project.interactivenovelplatform.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChapterOrderUpdateRequestDto {
    private Long chapterId;
    private Double newChapterNumber;
}
