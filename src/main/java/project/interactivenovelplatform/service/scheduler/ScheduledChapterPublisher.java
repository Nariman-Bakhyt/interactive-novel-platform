package project.interactivenovelplatform.service.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.repository.ChapterRepository;
import project.interactivenovelplatform.repository.NovelRepository;
import project.interactivenovelplatform.entity.ChapterEntity;
import project.interactivenovelplatform.entity.ChapterStatus;
import project.interactivenovelplatform.entity.NovelEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledChapterPublisher {
    private final ChapterRepository chapterRepository;
    private final NovelRepository novelRepository;

    @Scheduled(cron = "0 * * * * *") 
    @Transactional
    public void publishScheduledChapters() {
        OffsetDateTime now = OffsetDateTime.now();
        
        
        List<ChapterEntity> scheduledChapters = chapterRepository.findAllByStatusAndPublishedAtBeforeAndIsDeletedFalse(
                ChapterStatus.SCHEDULED, now
        );

        for (ChapterEntity chapter : scheduledChapters) {
            chapter.setStatus(ChapterStatus.PUBLISHED);
            chapterRepository.save(chapter);

            
            NovelEntity novel = chapter.getNovel();
            novel.setChapterCount(novel.getChapterCount() + 1);
            if (novel.getLastChapterAddedAt() == null || chapter.getPublishedAt().isAfter(novel.getLastChapterAddedAt())) {
                novel.setLastChapterAddedAt(chapter.getPublishedAt());
            }
            novelRepository.save(novel);
        }
    }
}
