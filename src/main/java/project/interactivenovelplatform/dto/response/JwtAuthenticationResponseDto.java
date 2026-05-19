package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtAuthenticationResponseDto {
    private String accessToken;
    private String tokenType = "Bearer";
    private String username;
    public JwtAuthenticationResponseDto(String accessToken, String username) {
        this.accessToken = accessToken;
        this.username = username;
        
    }
}
