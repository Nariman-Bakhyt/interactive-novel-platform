package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_close_friends")
@Getter @Setter
@NoArgsConstructor
public class UserCloseFriendsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="owner_id", nullable = false)
    private AppUserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id",nullable = false)
    private AppUserEntity friend;

    @JoinColumn(name = "added_at" , nullable = false)
    private OffsetDateTime addedAt = OffsetDateTime.now();
}
