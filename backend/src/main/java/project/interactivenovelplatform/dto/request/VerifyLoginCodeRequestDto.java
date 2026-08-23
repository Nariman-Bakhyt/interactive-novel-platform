package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class VerifyLoginCodeRequestDto {
    @NotBlank
    @Email
    String email;
    @NotBlank
    String code;
}
