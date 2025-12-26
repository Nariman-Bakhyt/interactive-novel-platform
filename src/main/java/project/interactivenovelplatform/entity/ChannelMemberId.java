package project.interactivenovelplatform.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChannelMemberId implements Serializable {
    private Long channelId;
    private Long userId;
}
