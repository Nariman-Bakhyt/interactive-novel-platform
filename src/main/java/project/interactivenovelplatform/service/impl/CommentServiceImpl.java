package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.simpleframework.xml.Default;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.request.CommentRequestDto;
import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.CommentResponseDto;
import project.interactivenovelplatform.dto.response.RatingStatsDto;
import project.interactivenovelplatform.entity.CommentEntity;
import project.interactivenovelplatform.entity.RatingEntity;
import project.interactivenovelplatform.repository.*;
import project.interactivenovelplatform.service.CommentService;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final RatingRepository ratingRepository;
    private final NovelRepository novelRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final ChapterBlockRepository chapterBlockRepository;
    private final ChapterRepository chapterRepository;

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

    private CommentResponseDto convertToResponse(CommentEntity entity) {
        return CommentResponseDto.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .timestamp(entity.getTimestamp())
                .userId(entity.getUser().getId())
                .username(entity.getUser().getUsername())
                .userAvatarUrl(entity.getUser().getAvatarUrl())

                .blockId(entity.getBlock() != null ? entity.getBlock().getId() : null)
                .chapterId(entity.getChapter() != null ? entity.getChapter().getId() : null)
                .novelId(entity.getNovel() != null ? entity.getNovel().getId() : null)
                .parentCommentId(entity.getParentComment() != null ? entity.getParentComment().getId() : null)
                .build();
    }




    @Transactional
    @Override
    public CommentResponseDto createComment(CommentRequestDto dto , String userName){
        var user = userRepository.findByUsernameIgnoreCase(userName)
                .orElseThrow(()->new EntityNotFoundException("Пользователь с именем: " + userName + " не найден"));
        CommentEntity commentEntity = new CommentEntity();
        commentEntity.setContent(dto.getContent());
        commentEntity.setTimestamp(OffsetDateTime.now());
        commentEntity.setUser(user);

        if (dto.getParentCommentId() != null){
            CommentEntity parent =  commentRepository.findById(dto.getParentCommentId())
                    .orElseThrow(()->new EntityNotFoundException("Родительский комментарий не найден"));
            commentEntity.setParentComment(parent);
        }

        if (dto.getBlockId() != null) {
            commentEntity.setBlock(chapterBlockRepository.getReferenceById(dto.getBlockId()));
        } else if (dto.getChapterId() != null) {
            commentEntity.setChapter(chapterRepository.getReferenceById(dto.getChapterId()));
        } else if (dto.getNovelId() != null) {
            commentEntity.setNovel(novelRepository.getReferenceById(dto.getNovelId()));
        } else {
            throw new IllegalArgumentException("Comment must have a target (block, chapter, or novel)");
        }
        // нужно дополнить форумами и тд
        var save = commentRepository.save(commentEntity);
        return convertToResponse(save);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponseDto> getComments(CommentRequestDto dto, Pageable pageable   ) {

        if (dto.getBlockId() != null) {
            return commentRepository.findByBlock_Id(dto.getBlockId(), pageable).map(this::convertToResponse);
        } else if (dto.getChapterId() != null) {
            return commentRepository.findByChapter_Id(dto.getChapterId(), pageable).map(this::convertToResponse);
        } else if (dto.getNovelId() != null) {
            return commentRepository.findByNovel_Id(dto.getNovelId(), pageable).map(this::convertToResponse);
        }

        return Page.empty();
    }

}
