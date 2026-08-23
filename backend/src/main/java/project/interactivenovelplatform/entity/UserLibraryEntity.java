package project.interactivenovelplatform.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;


@Entity
@Table(name = "user_library")
@Getter@Setter
@NoArgsConstructor
public class UserLibraryEntity {
    @EmbeddedId
    private UserNovelId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private AppUserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("novelId")
    @JoinColumn(name = "novel_id", nullable = false)
    private NovelEntity novel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LibraryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_level")
    private PrivacyLevel privacyLevel = PrivacyLevel.NOBODY;

    @Column(name = "created_at")
    private OffsetDateTime createdAt =  OffsetDateTime.now();
}
