package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import project.interactivenovelplatform.entity.ChapterContentType;

@Getter
@Setter
@AllArgsConstructor
public class ChapterBlockRequestDto {
    private Long id;
    @NotNull
    @Min(1)
    private Integer sequenceOrder;
    @NotNull
    private ChapterContentType type;
    @NotBlank
    private String content;
}
