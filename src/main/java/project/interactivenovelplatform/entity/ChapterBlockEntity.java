package project.interactivenovelplatform.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name= "chapter_blocks" , uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chapter_id","sequence_order"})
})
@Getter
@Setter
@NoArgsConstructor
public class ChapterBlockEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(name = "sequence_order", nullable=false)
    private Integer sequenceOrder ;

    @Enumerated(EnumType.STRING)
    @Column(name = "type",nullable=false)
    private ChapterContentType type;

    @Column(name = "content",nullable=false,columnDefinition = "TEXT")
    private String content ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private ChapterEntity chapter ;
}
