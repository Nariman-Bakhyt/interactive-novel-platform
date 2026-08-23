package project.interactivenovelplatform.error;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        String message,
        String detailedMessage,
        LocalDateTime errorTime
) { }
