package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChapterShortResponseDto {
    private Long id;
    private Double chapterNumber;
    private String title;
}
