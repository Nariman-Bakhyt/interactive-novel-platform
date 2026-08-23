package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reading_history")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReadingHistoryEntity {
    @EmbeddedId
    private UserNovelId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private AppUserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("novelId")
    @JoinColumn(name = "novel_id")
    private NovelEntity novel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_chapter_id")
    private ChapterEntity lastChapter;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

}
