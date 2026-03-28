package project.interactivenovelplatform.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;


@Entity
@Table(name = "user_library")
@Getter@Setter
@NoArgsConstructor
public class UserLibraryEntity {
    @EmbeddedId
    private UserLibraryId userLibraryId;

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

    @Column(name = "is_private")
    private boolean isPrivate = true;
}
