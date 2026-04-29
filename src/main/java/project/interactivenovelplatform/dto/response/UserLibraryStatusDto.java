package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.interactivenovelplatform.entity.LibraryStatus;

@Getter
@Setter
@AllArgsConstructor @NoArgsConstructor
public class UserLibraryStatusDto {
    private Long novelId;
    private LibraryStatus status;
}
