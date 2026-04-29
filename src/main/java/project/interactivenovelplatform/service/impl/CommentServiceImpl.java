package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.CommentRequestDto;
import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.AllRatingResponseDto;
import project.interactivenovelplatform.dto.response.AllRatingsResponseDto;
import project.interactivenovelplatform.dto.response.CommentResponseDto;
import project.interactivenovelplatform.dto.response.RatingResponseDto;
import project.interactivenovelplatform.entity.CommentEntity;
import project.interactivenovelplatform.entity.Metadata;
import project.interactivenovelplatform.entity.RatingEntity;
import project.interactivenovelplatform.repository.CommentRepository;
import project.interactivenovelplatform.repository.RatingRepository;
import project.interactivenovelplatform.security.UrlValidator;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.CommentService;
import project.interactivenovelplatform.service.NovelService;
import project.interactivenovelplatform.service.StorageService;
import project.interactivenovelplatform.service.UserService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final RatingRepository ratingRepository;
    private final NovelService novelService;
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final StorageService storageService;

    private final TransactionTemplate transactionTemplate;


    @Transactional
    @Override
    public RatingResponseDto setRating(Long novelId, Long userId, RatingRequestDto dto){
        var novel = novelService.getNovelEntity(novelId);
        var user = userService.getEntityIsActiveAndIsLockedFalse(userId);
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
    @Transactional(readOnly = true)
    public AllRatingsResponseDto getRatings(Long novelId, Pageable pageable){
        var novel = novelService.getNovelById(novelId);
        var ratings =ratingRepository.findByNovelId(novelId, pageable);
        Page<AllRatingResponseDto>  allRatingResponseDtoPage = ratings.map(rating -> new AllRatingResponseDto(
                rating.getId(),
                rating.getCommentText(),
                rating.getUser().getUsername(),
                rating.getTimestamp(),
                rating.getScore()));

        return new AllRatingsResponseDto( novel.getTotalScore(),novel.getRatingCount(),
                calculateAverage(novel.getRatingCount(),novel.getTotalScore()),
                new PagedModel<>(allRatingResponseDtoPage)
        );

    }
    private double calculateAverage(Integer ratingCount,Long totalScore){
        if(ratingCount == 0){
            return 0.0;
        }
        double average = (double) totalScore / ratingCount;
        return Math.round(average * 100.0) / 100.0;
    }

    @Transactional
    @Override
    public RatingResponseDto deleteRating(Long novelId,Long ratingId, Long userId){
        var novel = novelService.getNovelById(novelId);
        RatingEntity rating = ratingRepository.findByUserIdAndNovelId(userId, novelId)
                .orElseThrow(() -> new EntityNotFoundException("вы не писали рейтинг в новелле с id "+novelId));
        if(!rating.getId().equals(ratingId)) { throw new IllegalArgumentException("неверный id рейтинга "+ratingId); }
        ratingRepository.updateNovelStats(novelId, -rating.getScore(), -1);
        ratingRepository.delete(rating);

        return new RatingResponseDto(
                rating.getId(),
                novel.getTotalScore(),
                novel.getRatingCount(),
                calculateAverage(novel.getRatingCount(),novel.getTotalScore()),
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
                .metadata(entity.getMetadata())

                .blockId(entity.getBlock() != null ? entity.getBlock().getId() : null)
                .chapterId(entity.getChapter() != null ? entity.getChapter().getId() : null)
                .novelId(entity.getNovel() != null ? entity.getNovel().getId() : null)
                .parentCommentId(entity.getParentComment() != null ? entity.getParentComment().getId() : null)
                .build();
    }




    @Override
    public CommentResponseDto createComment(List<MultipartFile> files, CommentRequestDto dto , UserPrincipal principal){
        CommentEntity commentEntity = transactionTemplate.execute(_ ->
                saveCommentSkeleton(dto, principal.getId())
        );

        try {
            String datePath = commentEntity.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Metadata metadata = createMetadata(files, datePath, dto, principal.getId());
            CommentEntity finalEntity = transactionTemplate.execute(_ -> updateCommentMetadata(metadata, commentEntity.getId()));
            return convertToResponse(finalEntity);
        } catch (Exception e) {
            transactionTemplate.execute(_ -> {
                commentRepository.delete(commentEntity);
                return null;
            });
            throw e;
        }

    }

    private CommentEntity saveCommentSkeleton(CommentRequestDto dto , Long userId){
        var user = userService.getEntityIsActiveAndIsLockedFalse(userId);
        CommentEntity commentEntity = new CommentEntity();
        commentEntity.setContent(dto.getContent());
        commentEntity.setTimestamp(OffsetDateTime.now());
        commentEntity.setUser(user);
        setCommentTarget(commentEntity, dto);

        return commentRepository.saveAndFlush(commentEntity);
    }

    private Metadata createMetadata(List<MultipartFile> files, String datePath , CommentRequestDto dto , Long userId){
        Metadata metadata = new Metadata();
        List<String> imageUrls = new ArrayList<>();
        try {
            if ("IMAGE".equals(dto.getType())) {
                if(files!=null && !files.isEmpty()) {
                    if (files.size() > 10) throw new RuntimeException("Too many files!");
                    for (MultipartFile file : files) {
                        String actualMimeType = storageService.verifyRealImageType(file);

                        String secureExtension = MimeTypes.getDefaultMimeTypes()
                                .forName(actualMimeType)
                                .getExtension();
                        String folderPath = String.format("comments/users/%d/%s", userId, datePath);
                        String finalFileName = UUID.randomUUID().toString().substring(0, 8) + secureExtension;
                        String url = storageService.uploadFile(file, folderPath, finalFileName);
                        imageUrls.add(url);
                    }
                    metadata.setType(dto.getType());
                    metadata.setImages(imageUrls);
                }
                else {
                    throw new IllegalArgumentException("нету фото");
                }
            }
            else if("QUOTE".equals(dto.getType())){
                if (dto.getQuoteText() == null || dto.getQuoteText().isBlank() ||
                        dto.getAnchorUrl() == null || dto.getAnchorUrl().isBlank()) {
                    throw new BadRequestException("Для цитаты необходим текст и ссылка на источник");
                }
                if (!UrlValidator.isTrusted(dto.getAnchorUrl())) {
                    throw new BadRequestException("Ссылка ведет на недоверенный ресурс");
                }
                // 2. Формируем чистую ссылку с Query-параметром ?q=
                String rawText = dto.getQuoteText();
                // Кодируем текст (пробелы станут %20, а не плюсики)
                String encodedText = URLEncoder.encode(rawText, StandardCharsets.UTF_8).replace("+", "%20");

                String finalUrl = dto.getAnchorUrl();

                // Если в anchorUrl уже есть параметры (содержит ?), добавляем через &, если нет — через ?
                String separator = finalUrl.contains("?") ? "&" : "?";
                finalUrl += separator + "q=" + encodedText;

                // 3. Сохраняем в метаданные
                metadata.setType("QUOTE");
                metadata.setQuoteText(rawText);
                metadata.setAnchorUrl(finalUrl);

            }
            else {
                metadata.setType("PLAIN");
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return  metadata;
    }

    private CommentEntity updateCommentMetadata(Metadata metadata, Long commentId ){
        var commentEntity = commentRepository.findById(commentId)
                .orElseThrow(()->new EntityNotFoundException("комментарий не найден"));
        commentEntity.setMetadata(metadata);
        return commentRepository.save(commentEntity);
    }

    public void setCommentTarget(CommentEntity commentEntity, CommentRequestDto dto) {
        if (dto.getParentCommentId() != null){
            CommentEntity parent =  commentRepository.findById(dto.getParentCommentId())
                    .orElseThrow(()->new EntityNotFoundException("Родительский комментарий не найден"));
            commentEntity.setParentComment(parent);
        }

        if (dto.getBlockId() != null) {
            commentEntity.setBlock(novelService.getBlockReference(dto.getBlockId()));
        } else if (dto.getChapterId() != null) {
            commentEntity.setChapter(novelService.getChapterReference(dto.getChapterId()));
        } else if (dto.getNovelId() != null) {
            commentEntity.setNovel(novelService.getNovelReference(dto.getNovelId()));
        } else {
            throw new IllegalArgumentException("Comment must have a target (block, chapter, or novel)");
        }
        // нужно дополнить форумами и тд
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
