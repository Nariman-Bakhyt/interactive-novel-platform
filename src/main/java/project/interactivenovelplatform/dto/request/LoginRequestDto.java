package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginRequestDto {
    @NotBlank
    private String username;
    @NotBlank
    @Size(min = 8,message = "Пароль должен быть не менее 8 символов.")
    private String password;
}
