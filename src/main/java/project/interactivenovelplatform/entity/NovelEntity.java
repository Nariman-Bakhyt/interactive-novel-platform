package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;

@Entity
@Table(name = "novel")
@Getter
@Setter
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
    @Column(name = "description")
    private String description;
    @Column(name = "publication_date")
    private ZonedDateTime publicationDate;
    @Column(name = "chapter_count",nullable = false)
    private int chapterCount = 0;
    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    @Column(name = "rating_count",nullable = false)
    private int ratingCount = 0;
    @Column(name = "view_count",nullable = false)
    private Long viewCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "author_id",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_novel_author")
    )
    private AppUserEntity author;
}
