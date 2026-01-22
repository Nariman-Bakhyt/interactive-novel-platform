package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import project.interactivenovelplatform.dto.request.ChapterBlockRequestDto;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ChapterResponseDto {
    private Long id;
    private Double chapterNumber;
    private String title;
    private List<ChapterBlockRequestDto> blocks;
}
