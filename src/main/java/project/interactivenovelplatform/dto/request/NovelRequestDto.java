package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class NovelRequestDto {
    @NotBlank
    @Size
    String title;
    @NotBlank
    String status;
    String description;
    @NotNull(message = "Author ID должен быть указан.")
    Long authorId;
}
