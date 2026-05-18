package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name= "chapter",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_novel_chapter_number",
                    columnNames = {"novel_id","chapter_number"}
            )
        }
)
@SQLRestriction("is_deleted = false")
@Getter @Setter
@NoArgsConstructor
public class ChapterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_number",nullable = false)
    private Double chapterNumber;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "created_at",nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    private List<ChapterBlockEntity> blocks = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id",nullable = false)
    private NovelEntity novel;
}