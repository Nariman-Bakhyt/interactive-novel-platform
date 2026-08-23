package project.interactivenovelplatform.dto.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequestDto {
    Long userId;
    String newPassword;

}
