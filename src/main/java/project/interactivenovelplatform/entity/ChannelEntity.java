package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "channel")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue(SubscribableType.Values.CHANNEL)
@Getter
@Setter
public class ChannelEntity extends SubscribableEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "is_private")
    private boolean isPrivate = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUserEntity author;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
