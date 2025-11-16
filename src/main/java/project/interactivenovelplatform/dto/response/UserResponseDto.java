package project.interactivenovelplatform.dto.response;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserResponseDto {
    @NotBlank
    private Long id;
    @NotBlank
    @Size(min = 5, max = 50, message = "Логин должен содержать от 4 до 50 символов.")
    private String username;
    @NotBlank(message = "Email не может быть пустым.")
    @Email(message = "Некорректный формат email адреса.")
    private String email;

    public UserResponseDto(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

}
