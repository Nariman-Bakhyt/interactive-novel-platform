package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import project.interactivenovelplatform.entity.RelationStatus;

import java.time.OffsetDateTime;

@Getter @Setter
@AllArgsConstructor
public class UserRelationResponseDto {

    private Long id;

    private Long userId;
    private String username;
    private String avatarUrl;

    private RelationStatus status;
    private OffsetDateTime updatedAt;
}
