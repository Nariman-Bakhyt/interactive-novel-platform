package project.interactivenovelplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerificationCacheDto {
    private String code;
    private String pendingValue;
}
