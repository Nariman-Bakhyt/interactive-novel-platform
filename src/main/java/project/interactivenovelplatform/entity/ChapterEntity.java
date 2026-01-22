package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Getter
@Setter
@NoArgsConstructor
public class ChapterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_number",nullable = false)
    private Double chapterNumber;

    @Column(name = "title", nullable = false)
    private String title;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    private List<ChapterBlockEntity> blocks = new ArrayList<>();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id",nullable = false)
    private NovelEntity novel;
}
