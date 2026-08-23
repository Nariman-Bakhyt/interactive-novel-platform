package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponseDto {
    private Long id;
    private String username;
    private String email;
    private OffsetDateTime registrationDate;
    private Set<String> roles = new HashSet<>(); 
    private Boolean isDeleted = Boolean.FALSE;
    private Boolean isLocked = false;       
    private OffsetDateTime  lockTime;         
    private String avatarUrl;
}
