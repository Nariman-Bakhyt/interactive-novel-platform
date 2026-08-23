package project.interactivenovelplatform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "rating",
        uniqueConstraints = {
                @UniqueConstraint(name = "rating_user_id_novel_id_key", columnNames = {"user_id", "novel_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RatingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(value = 1, message = "Оценка не может быть меньше 1")
    @Max(value = 5, message = "Оценка не может быть больше 5")
    @Column(name = "score",nullable = false)
    private int score;

    @Column(name = "comment_text",length = 3000)
    private String commentText;

    @Column(name = "timestamp")
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private AppUserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id" , nullable = false)
    private NovelEntity novel;

}
