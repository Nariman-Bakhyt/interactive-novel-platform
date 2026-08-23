package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class NovelRequestDto {
    @NotBlank
    @Size(min = 3, max = 255, message = "название должно содержать от 3 до 255 символов.")
    private String title;
    @NotBlank
    private String status;
    private String description;
    private MultipartFile coverImage;

    private List<Long> tags;
    private List<Long> genres;

}
