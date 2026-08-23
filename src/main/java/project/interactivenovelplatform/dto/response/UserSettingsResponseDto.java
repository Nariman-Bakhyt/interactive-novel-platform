package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import project.interactivenovelplatform.entity.PrivacyLevel;

@Getter @Setter
@AllArgsConstructor
public class UserSettingsResponseDto {
    private PrivacyLevel canSendMessage;
    private PrivacyLevel libraryPrivacy;
}
