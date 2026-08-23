package project.interactivenovelplatform.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import project.interactivenovelplatform.entity.LibraryStatus;
import project.interactivenovelplatform.entity.PrivacyLevel;

@Getter @Setter
@AllArgsConstructor
public class UserLibraryRequestDto {
    @Min(1)
    private Long novelId;
    @NotNull
    private LibraryStatus status;
    @NotNull
    private PrivacyLevel privacyLevel;
}
