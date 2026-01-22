package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.RatingStatsDto;
import project.interactivenovelplatform.entity.NovelEntity;
import project.interactivenovelplatform.entity.RatingEntity;
import project.interactivenovelplatform.repository.NovelRepository;
import project.interactivenovelplatform.repository.RatingRepository;
import project.interactivenovelplatform.repository.UserRepository;
import project.interactivenovelplatform.service.RatingService;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {
    private final RatingRepository ratingRepository;
    private final NovelRepository novelRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public RatingStatsDto setRating(Long novelId, Long userId, RatingRequestDto dto){
        var novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new EntityNotFoundException("Роман с Id:"+ novelId +" не найден."));
        var user= userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с Id:"+userId+" не найден"));
        if(novel.getAuthor().getId().equals(userId)) throw new IllegalArgumentException("Автор не может сам себя оценивать");

        ratingRepository.findByUserIdAndNovelId(userId, novelId).ifPresentOrElse(existingRating -> {
                    int oldScore = existingRating.getScore();
                    int scoreDiff = dto.getScore()-oldScore;
                    existingRating.setScore(dto.getScore());
                    existingRating.setCommentText(dto.getCommentText());
                    existingRating.setTimestamp(OffsetDateTime.now());
                    ratingRepository.updateNovelStats(novelId, scoreDiff, 0);
                    novel.setTotalScore(novel.getTotalScore()+scoreDiff);
                    },
                ()->{
                    RatingEntity newRating = new RatingEntity();
                    newRating.setNovel(novel);
                    newRating.setUser(user);
                    newRating.setScore(dto.getScore());
                    newRating.setCommentText(dto.getCommentText());
                    ratingRepository.save(newRating);
                    ratingRepository.updateNovelStats(novelId, dto.getScore(), 1);
                    novel.setTotalScore(novel.getTotalScore()+dto.getScore());
                    novel.setRatingCount(novel.getRatingCount()+1);
                }
        );

        return new RatingStatsDto(
                novel.getTotalScore(),
                novel.getRatingCount(),
                novel.calculateAverage()
        );

    }

    @Transactional
    @Override
    public RatingStatsDto deleteRating(Long novelId, Long userId){
        var novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new EntityNotFoundException("Роман с Id:"+ novelId +" не найден."));
        ratingRepository.findByUserIdAndNovelId(userId, novelId)
                .ifPresent(rating -> {
                    novel.setTotalScore(novel.getTotalScore()-rating.getScore());
                    novel.setRatingCount(novel.getRatingCount()-1);
                    ratingRepository.updateNovelStats(novelId, -rating.getScore(), -1);
                    ratingRepository.delete(rating);

                }
        );
        return new RatingStatsDto(
                novel.getTotalScore(),
                novel.getRatingCount(),
                novel.calculateAverage()
        );
    }

}
