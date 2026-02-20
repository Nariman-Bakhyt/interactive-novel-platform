package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class NovelResponseDto {
    private Long id;
    private String title;
    private String status;
    private String description;
    private OffsetDateTime publicationDate;
    private Integer chapterCount;
    private Long totalScore;
    private Integer ratingCount;
    private Long viewCount;
    private String authorName;
    private String coverUrl;
    private List<TagOrGenreResponseDto> tags;
    private List<TagOrGenreResponseDto> genres;
}
