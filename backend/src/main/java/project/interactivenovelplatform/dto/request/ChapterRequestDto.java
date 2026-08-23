package project.interactivenovelplatform.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChapterRequestDto {
    @NotBlank
    @Size(min = 2, max = 255)
    private String title;

    @NotNull
    @Valid
    private List<ChapterBlockRequestDto> blocks;

    private OffsetDateTime publishedAt;
}
