package project.interactivenovelplatform.dto.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import project.interactivenovelplatform.entity.PrivacyLevel;

@Getter
@Setter
@AllArgsConstructor
public class UserSettingsRequestDto {
    private PrivacyLevel canSendMessage;
    private PrivacyLevel libraryPrivacy;

}
