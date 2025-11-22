package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserUpdateRequestDto {
    @NotBlank
    @Size(min = 4, max = 50, message = "Логин должен содержать от 4 до 50 символов.")
    private String newUsername;
    @NotBlank(message = "Email не может быть пустым.")
    @Email(message = "Некорректный формат email адреса.")
    private String newEmail;
}
