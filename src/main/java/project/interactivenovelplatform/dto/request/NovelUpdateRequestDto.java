package project.interactivenovelplatform.dto.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class NovelUpdateRequestDto {
    private String title;
    private String status;
    private String description;
    private List<Long> tags;
    private List<Long> genres;
}
