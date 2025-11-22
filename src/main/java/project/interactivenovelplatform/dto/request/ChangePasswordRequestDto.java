package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
public class ChangePasswordRequestDto {
    @NotBlank
    @Size(min = 8,message = "Пароль должен быть не менее 8 символов.")
    private String oldPassword;
    @NotBlank
    @Size(min = 8,message = "Пароль должен быть не менее 8 символов.")
    private String newPassword;
}
