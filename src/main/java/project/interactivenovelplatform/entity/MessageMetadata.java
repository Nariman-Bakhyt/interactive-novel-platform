package project.interactivenovelplatform.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MessageMetadata(
        String type,

        @NotBlank
        String quoteText,

        @NotBlank
        @Pattern(regexp = "^/.*", message = "Path must be relative and start with /")
        String targetPath
) {
}
