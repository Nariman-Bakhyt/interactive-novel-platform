package project.interactivenovelplatform.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.interactivenovelplatform.entity.Novel;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NovelSearchRequestDto {
    private String title;
    private Long authorId;
    private List<Long> includedGenreIds;
    private List<Long> excludedGenreIds;
    private List<Long> includedTagIds;
    private List<Long> excludedTagIds;
    private Double minRating;
    private Double maxRating;
    private Novel status;
}
