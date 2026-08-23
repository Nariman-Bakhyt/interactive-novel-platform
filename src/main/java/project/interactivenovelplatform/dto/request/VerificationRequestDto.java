package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import project.interactivenovelplatform.entity.VerificationTokenType;

@Getter@Setter
@RequiredArgsConstructor
public class VerificationRequestDto {
    private Long userId;
    @NotNull
    private VerificationTokenType type;
    @NotBlank
    private String code;
}
