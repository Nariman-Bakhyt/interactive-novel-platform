package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "novel")
@org.hibernate.annotations.SQLDelete(sql = "UPDATE novel SET is_deleted = true WHERE id = ?")
@Getter @Setter
@NoArgsConstructor
public class NovelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "title",nullable = false)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    private Novel status = Novel.IN_PROGRESS;
    @Column(name = "description",length = 2000)
    private String description;
    @Column(name = "created_at")
    private OffsetDateTime createdAt =  OffsetDateTime.now();
    @Column(name = "publication_date")
    private OffsetDateTime publicationDate = OffsetDateTime.now();
    @Column(name = "last_chapter_added_at")
    private OffsetDateTime lastChapterAddedAt;
    @Column(name = "chapter_count",nullable = false)
    private Integer chapterCount = 0;
    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount = 0;
    @Column(name = "total_score",nullable = false)
    private Long totalScore = 0L;
    @Column(name = "average_rating",columnDefinition = "numeric(3,2)",nullable = false)
    private Double averageRating = 0D;
    @Column(name = "view_count",nullable = false)
    private Long viewCount = 0L;
    @Column(name = "cover_url", length = 512)
    private String coverUrl;
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "author_id",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_novel_author")
    )
    private AppUserEntity author;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "novel_genre",
            joinColumns = @JoinColumn(name = "novel_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<GenreEntity> genres = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "novel_tag",
            joinColumns = @JoinColumn(name = "novel_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TagEntity> tags = new HashSet<>();

    public double calculateAverage(){
        if(ratingCount == 0){
            return 0.0;
        }
        double average = (double) totalScore / ratingCount;
        return Math.round(average * 100.0) / 100.0;
    }
}
