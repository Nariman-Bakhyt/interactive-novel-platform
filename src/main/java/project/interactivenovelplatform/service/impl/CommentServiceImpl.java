package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.request.CommentRequestDto;
import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.AllRatingResponseDto;
import project.interactivenovelplatform.dto.response.AllRatingsResponseDto;
import project.interactivenovelplatform.dto.response.CommentResponseDto;
import project.interactivenovelplatform.dto.response.RatingResponseDto;
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
    public RatingResponseDto setRating(Long novelId, Long userId, RatingRequestDto dto){
        var novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new EntityNotFoundException("Роман с Id:"+ novelId +" не найден."));
        var user= userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с Id:"+userId+" не найден"));
        if(novel.getAuthor().getId().equals(userId)) throw new IllegalArgumentException("Автор не может сам себя оценивать");
        var timestamp = OffsetDateTime.now();

        RatingEntity rating = ratingRepository.findByUserIdAndNovelId(userId, novelId)
                .orElseGet(() -> {
                    RatingEntity newEntity = new RatingEntity();
                    newEntity.setNovel(novel);
                    newEntity.setUser(user);
                    return newEntity;
                });

        if (rating.getId() != null) {
            int oldScore = rating.getScore();
            int scoreDiff = dto.getScore() - oldScore;

            ratingRepository.updateNovelStats(novelId, scoreDiff, 0);
        } else {
            ratingRepository.updateNovelStats(novelId, dto.getScore(), 1);
        }

        rating.setScore(dto.getScore());
        rating.setCommentText(dto.getCommentText());
        rating.setTimestamp(timestamp);

        RatingEntity savedRating = ratingRepository.save(rating);

        return new RatingResponseDto(
                savedRating.getId(),
                novel.getTotalScore()+dto.getScore(),
                novel.getRatingCount()+1,
                novel.calculateAverage(),
                dto.getCommentText(),
                user.getUsername(),
                timestamp,
                dto.getScore()
        );
    }
    @Override
    public AllRatingsResponseDto getRatings(Long novelId, Pageable pageable){
        var novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new EntityNotFoundException("Роман с Id:"+ novelId +" не найден."));
        var ratings = ratingRepository.findByNovelId(novelId, pageable);
        return new AllRatingsResponseDto( novel.getTotalScore(),novel.getRatingCount(),novel.calculateAverage(),ratings.map(rating -> new AllRatingResponseDto(
                rating.getId(),
                rating.getCommentText(),
                rating.getUser().getUsername(),
                rating.getTimestamp(),
                rating.getScore()
        )));

    }

    @Transactional
    @Override
    public RatingResponseDto deleteRating(Long novelId,Long ratingId, Long userId){
        var novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new EntityNotFoundException("Роман с Id:"+ novelId +" не найден."));
        RatingEntity rating = ratingRepository.findByUserIdAndNovelId(userId, novelId)
                .orElseThrow(() -> new EntityNotFoundException("вы не писали рейтинг в новелле с id "+novelId));
        if(!rating.getId().equals(ratingId)) { throw new IllegalArgumentException("неверный id рейтинга "+ratingId); }
        ratingRepository.updateNovelStats(novelId, -rating.getScore(), -1);
        ratingRepository.delete(rating);

        return new RatingResponseDto(
                rating.getId(),
                novel.getTotalScore(),
                novel.getRatingCount(),
                novel.calculateAverage(),
                null,
                null,
                null,
                rating.getScore()
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
            return commentRepository.findByBlock_IdAndIsDeletedIsFalse(dto.getBlockId(), pageable).map(this::convertToResponse);
        } else if (dto.getChapterId() != null) {
            return commentRepository.findByChapter_IdAndIsDeletedFalse(dto.getChapterId(), pageable).map(this::convertToResponse);
        } else if (dto.getNovelId() != null) {
            return commentRepository.findByNovel_IdAndIsDeletedFalse(dto.getNovelId(), pageable).map(this::convertToResponse);
        }

        return Page.empty();
    }

    @Override
    @Transactional
    public CommentResponseDto deleteComment(Long commentId , String userName){
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Комментарий не найден"));

        if (!comment.getUser().getUsername().equals(userName)) {
            throw new AccessDeniedException("Вы не являетесь автором этого комментария");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);
        return convertToResponse(comment);
    }

}
