package project.interactivenovelplatform.dto.response;


import lombok.*;
import org.springframework.http.ResponseCookie;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDto {
    String accessToken;
    String refreshToken;
    ResponseCookie cookie;
    ResponseCookie guestCookie;
    String username;
}
