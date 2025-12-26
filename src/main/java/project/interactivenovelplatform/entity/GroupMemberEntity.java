package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "group_member")
@Getter
@Setter
@NoArgsConstructor
public class GroupMemberEntity {
    @EmbeddedId
    private GroupMemberId groupMemberId = new GroupMemberId();

    @Enumerated(EnumType.STRING)
    @Column(name = "role",nullable = false)
    private ChannelRole role = ChannelRole.SUBSCRIBER;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id")
    private ChannelEntity chatGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private AppUserEntity user;
}
