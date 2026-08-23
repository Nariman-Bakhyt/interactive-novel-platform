package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class NovelAndChapterShortResponseDto {
    private NovelResponseDto novel;
    private List<ChapterShortResponseDto> chapters;
}
