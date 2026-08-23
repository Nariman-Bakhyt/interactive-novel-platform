package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserNameRequestDto {
    @NotEmpty
    @Size(min = 4, max = 50, message = "Логин должен содержать от 4 до 50 символов.")
    private String userName;
}
