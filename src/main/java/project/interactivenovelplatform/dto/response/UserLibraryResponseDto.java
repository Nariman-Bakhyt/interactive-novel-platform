package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import project.interactivenovelplatform.entity.LibraryStatus;
import project.interactivenovelplatform.entity.PrivacyLevel;

import java.time.OffsetDateTime;

@Getter@Setter
@AllArgsConstructor
public class UserLibraryResponseDto {
    private NovelResponseDto  novel;
    private LibraryStatus status;
    private OffsetDateTime createdAt;
    private PrivacyLevel privacyLevel;
}
