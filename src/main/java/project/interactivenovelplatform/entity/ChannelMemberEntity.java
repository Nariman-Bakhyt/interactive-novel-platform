package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "channel_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMemberEntity {
    @EmbeddedId
    private ChannelMemberId id = new ChannelMemberId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("channelId")
    @JoinColumn(name = "channel_id")
    private ChannelEntity channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private AppUserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role",nullable = false)
    private ChannelRole role = ChannelRole.SUBSCRIBER;

}
