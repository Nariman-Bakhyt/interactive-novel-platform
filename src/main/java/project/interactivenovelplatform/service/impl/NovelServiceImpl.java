package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.*;
import project.interactivenovelplatform.dto.response.*;
import project.interactivenovelplatform.entity.*;
import project.interactivenovelplatform.error.GlobalExceptionHandler;
import project.interactivenovelplatform.repository.*;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.*;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@AllArgsConstructor
@Service
public class NovelServiceImpl implements NovelService {
    private final UserService userService;
    private final TagAndGenreService tagAndGenreService;
    private final StorageService storageService;
    private final NovelRepository novelRepository;
    private static final Collection<Novel> NON_PUBLIC_STATUSES =
            List.of(Novel.DRAFT, Novel.ARCHIVED, Novel.RETRACTED);
    private final ChapterRepository chapterRepository;
    private final ChapterBlockRepository chapterBlockRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final EntityManager entityManager;

    private final static Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final TransactionTemplate transactionTemplate;
    private final StringRedisTemplate redisTemplate;
    private final StorageHelper storageHelper;

    private NovelResponseDto convertToDto(NovelEntity novel) {
        String publicCoverUrl = novel.getCoverUrl() != null ? storageService.getPublicUrl(novel.getCoverUrl()) : null;
        return new NovelResponseDto(
                novel.getId(),
                novel.getTitle(),
                novel.getStatus().toString(),
                novel.getDescription(),
                novel.getPublicationDate(),
                novel.getChapterCount(),
                novel.getTotalScore(),
                novel.getRatingCount(),
                novel.getViewCount(),
                novel.getAuthor().getUsername(),
                storageHelper.getCoverOrDefault(publicCoverUrl),
                novel.getTags().stream().map(tag -> new TagOrGenreResponseDto(
                        tag.getId(),
                        tag.getName()
                )).toList(),
                novel.getGenres().stream().map(genre -> new TagOrGenreResponseDto(
                        genre.getId(),
                        genre.getName()
                )).toList()
        );
    }
    private NovelAndChapterShortResponseDto convertToDtoFull(NovelEntity novel,List<ChapterShortResponseDto> chapters) {
        return new NovelAndChapterShortResponseDto(
                convertToDto(novel),
                chapters
        );
    }
    private ChapterResponseDto ChapterConvertToDto(ChapterEntity chapter) {
        return new ChapterResponseDto(
          chapter.getId(),
          chapter.getChapterNumber(),
          chapter.getTitle(),
          chapter.getBlocks().stream().map(blockEntity -> new ChapterBlockResponseDto(
                  blockEntity.getId(),
                  blockEntity.getSequenceOrder(),
                  blockEntity.getType(),
                  blockEntity.getContent()
          )).toList()
        );
    }


    private Novel getStatusFromString(String statusName) {
        try {
            return Novel.valueOf(statusName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неверный статус: " + statusName);
        }
    }
    @Transactional(readOnly = true)
    public boolean isAuthor(Long novelId, UserPrincipal userPrincipal) {
        if(userPrincipal == null) return false;
        return novelRepository.existsByIdAndAuthorId(novelId, userPrincipal.getId());
    }

    @Override
    public NovelResponseDto create(NovelRequestDto dto, Long currentAuthorId){

        OffsetDateTime now = OffsetDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String folderPath = String.format("covers/users/%d/%s", currentAuthorId, datePath);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String newCoverUrl = null;
        var file = dto.getCoverImage();
        if (file != null && !file.isEmpty()) {
            try {
                String actualMimeType = storageService.verifyRealImageType(file);
                String secureExtension = MimeTypes.getDefaultMimeTypes()
                        .forName(actualMimeType).getExtension();

                String filename = timestamp + secureExtension;
                newCoverUrl = storageService.uploadFile(file, folderPath, filename);
            } catch (Exception e) {
                log.warn("Не удалось загрузить обложку для автора {}: {}", currentAuthorId, e.getMessage());
            }
        }
        final String finalNewCoverUrl = newCoverUrl;
        try {
            NovelEntity savedNovel= transactionTemplate.execute(_ ->{
                        NovelEntity novelEntity = new NovelEntity();
                        novelEntity.setTitle(dto.getTitle());
                        novelEntity.setDescription(dto.getDescription());
                        novelEntity.setAuthor(userService.getEntityIsActiveAndIsLockedFalse(currentAuthorId));
                        novelEntity.setStatus(Novel.DRAFT);
                        novelEntity.setCreatedAt(now);
                        novelRepository.saveAndFlush(novelEntity);
                        tagAndGenreService.UpdateTagOrGenreToNovel(dto.getTags(),true,novelEntity);
                        tagAndGenreService.UpdateTagOrGenreToNovel(dto.getGenres(),false,novelEntity);
                        novelEntity.setCoverUrl(finalNewCoverUrl);
                        return novelEntity;
                    }
            );
            return convertToDto(savedNovel);

        }
        catch (Exception e) {
            if (finalNewCoverUrl != null) {
                storageService.deleteFile(finalNewCoverUrl);
            }
            throw new RuntimeException("Ошибка при создании новеллы: " + e.getMessage());
        }


    }
    @Override
    @Transactional(readOnly = true)
    public NovelAndChapterShortResponseDto findById(Long id,Long userId ) {
        var novel = novelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Роман не найден."));
        if(NON_PUBLIC_STATUSES.contains(novel.getStatus())) {
            if(!novel.getAuthor().getId().equals(userId)) throw new EntityNotFoundException("Роман не найден.");

        }
        var chapters =chapterRepository.findAllByNovelIdShort(novel.getId());
        return convertToDtoFull(novel,chapters);
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "lastChapterAddedAt",
            "averageRating",
            "viewCount",
            "chapterCount",
            "publicationDate"
    );

    @Override
    @Transactional(readOnly = true)
    public Page<NovelResponseDto> findAll(NovelSearchRequestDto dto, Pageable pageable) {

        // 1. Базовая спецификация (отсекаем удаленные и скрытые новеллы)
        Specification<NovelEntity> spec = (root, query, cb) -> {
            query.distinct(true);
            return cb.not(root.get("status").in(NON_PUBLIC_STATUSES));
        };

        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException("Сортировка по полю '" + order.getProperty() + "' запрещена!");
            }
        }

        // 2. Архитектурный переключатель сортировки
        // По умолчанию используем ту сортировку, которую прислал фронтенд (по дате, рейтингу и т.д.)
        Pageable effectivePageable = pageable;

        if (dto != null) {

            // ЕСЛИ ИДЕТ ПОИСК ПО ТЕКСТУ:
            if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
                spec = spec.and(NovelSpecifications.titleLike(dto.getTitle()));

                // Убиваем стандартную сортировку из Pageable, заменяя её на Sort.unsorted().
                // Это позволяет сработать сложной сортировке по релевантности (query.orderBy)
                // внутри спецификации titleLike без SQL-конфликтов.
                effectivePageable = PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.unsorted()
                );
            }

            // Добавляем остальные фильтры (они работают всегда)
            spec = spec.and(NovelSpecifications.hasAuthor(dto.getAuthorId()))
                    .and(NovelSpecifications.hasStatus(dto.getStatus()))
                    .and(NovelSpecifications.hasRatingInRange(dto.getMinRating(), dto.getMaxRating()))
                    .and(NovelSpecifications.filterByGenres(dto.getIncludedGenreIds(), dto.getExcludedGenreIds()))
                    .and(NovelSpecifications.filterByTags(dto.getIncludedTagIds(), dto.getExcludedTagIds()));
        }

        // 3. Шаг "Thin Page": Вытаскиваем и сортируем ТОЛЬКО идентификаторы (id)
        Page<NovelEntity> thinPage = novelRepository.findAll(spec, effectivePageable);

        if (thinPage.isEmpty()) {
            // Возвращаем пустую страницу с ОРИГИНАЛЬНЫМ pageable, чтобы фронт получил правильные метаданные
            return Page.empty(pageable);
        }

        List<Long> ids = thinPage.getContent().stream()
                .map(NovelEntity::getId)
                .toList();

        List<NovelEntity> richNovels = novelRepository.findAllByIdIn(ids);

        Map<Long, NovelEntity> novelMap = richNovels.stream()
                .collect(Collectors.toMap(NovelEntity::getId, n -> n));

        List<NovelResponseDto> content = ids.stream()
                .map(id -> convertToDto(novelMap.get(id)))
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, thinPage.getTotalElements());
    }
    @Override
    @Transactional
    public NovelResponseDto update(Long id, NovelUpdateRequestDto dto) {
        NovelEntity novel = novelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Роман с id: " + id + " не найден"));
        if(novel.getStatus() == Novel.RETRACTED) {
            throw new IllegalStateException("Нельзя редактировать роман, отозванный автором.");
        }
        // 1. Обновление Title:
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            novel.setTitle(dto.getTitle());
        }

        // 2. Обновление Description:
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            novel.setDescription(dto.getDescription());
        }

        // 3. Обновление Status:
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            Novel newStatus = getStatusFromString(dto.getStatus());
            if(newStatus == Novel.RETRACTED) {
                throw new IllegalStateException("Менять cтатус на отозванный нельзя.");
            }
            novel.setStatus(newStatus);
        }

        if(dto.getTags() != null ) {
            tagAndGenreService.UpdateTagOrGenreToNovel(dto.getTags(),true,novel);
        }

        if(dto.getGenres() != null ) {
            tagAndGenreService.UpdateTagOrGenreToNovel(dto.getGenres(),false,novel);
        }

        return convertToDto(novelRepository.save(novel));
    }

    @Override
    public NovelResponseDto updateCoverUrl(Long id, MultipartFile file, Principal principal){
        var novel = novelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("новелла не найдена"));
        String oldUrl = novel.getCoverUrl();

        // 2. Используем дату создания новеллы для пути (чтобы папка не менялась)
        String datePath = novel.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String folderPath = String.format("covers/users/%d/%s", novel.getAuthor().getId(), datePath);
        String newUrl = null;
        try {
            if (file != null && !file.isEmpty()) {
                String actualMimeType = storageService.verifyRealImageType(file);
                String extension = MimeTypes.getDefaultMimeTypes().forName(actualMimeType).getExtension();

                String filename = "novel_" + novel.getId() + "_rev" + System.currentTimeMillis() + extension;

                newUrl = storageService.uploadFile(file, folderPath, filename);
                final String finalUrl = newUrl;
                // 5. Обновляем БД
                var savedNovel = transactionTemplate.execute(_ ->saveNewCoverUrl(id,finalUrl));

                if (oldUrl != null && newUrl != null) {
                    storageService.deleteFile(oldUrl);
                }
                return convertToDto(savedNovel);
            }
            else {
                throw new EntityNotFoundException("нету файла");
            }
        } catch (Exception e) {
            if (newUrl != null) {
                storageService.deleteFile(newUrl);
            }
            throw new RuntimeException("Ошибка при обновлении обложки: " + e.getMessage(), e);
        }
    }

    public NovelEntity saveNewCoverUrl( Long novelId,String newCoverUrl){
        try {
            var novel = novelRepository.findById(novelId)
                    .orElseThrow(()->new EntityNotFoundException("новелла с ID: " + novelId + " не найден"));
            novel.setCoverUrl(newCoverUrl);
            return novelRepository.save(novel);
        }
        catch (Exception e) {
            throw new RuntimeException("Ошибка при обновлении обложки: " + e.getMessage(), e);
        }

    }

    @Override
    @Transactional(readOnly = true)
    public Page<NovelResponseDto> findNewNovels(int page , int size){
        Pageable pageable = PageRequest.of(page, size);
        return novelRepository.findAllByStatusNotInOrderByPublicationDateDesc(NON_PUBLIC_STATUSES,pageable).map(this::convertToDto);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<NovelResponseDto> findMyNovels(int page , int size,Long authorId){
        Pageable pageable = PageRequest.of(page, size);
        return novelRepository.findAllByAuthor_Id(authorId, pageable).map(this::convertToDto);
    }
    @Override
    @Transactional(readOnly = true)
    public NovelAndChapterShortResponseDto findMyNovel(Long id,Long authorId){
        var novel = novelRepository.findByAuthor_IdAndId(authorId,id).orElseThrow(
                ()->new EntityNotFoundException("новелла с ID: " + id + " не найден"));
        var chapters =chapterRepository.findAllByNovelIdShort(novel.getId());
        return convertToDtoFull(novel,chapters);
    }

    @Transactional
    @Override
    public ChapterResponseDto addChapter(Long novelId, ChapterRequestDto dto){
        var novel = novelRepository.findById(novelId).orElseThrow(
                ()->new EntityNotFoundException("Роман с id: " + novelId + " не найден"));

        if(chapterRepository.findByNovel_IdAndTitleIgnoreCase(novelId, dto.getTitle()).isPresent()) {
            throw new IllegalStateException("Глава с таким именем существует");
        }
        novel.setChapterCount(novel.getChapterCount()+1);
        novel.setLastChapterAddedAt(OffsetDateTime.now());
        novelRepository.save(novel);
        ChapterEntity chapter= new ChapterEntity();
        chapter.setNovel(novel);
        Double chapterNumber = chapterRepository.findMaxChapterNumberByNovelId(novelId)
                .map(max -> max + 1.0)
                .orElse(1.0);
        chapter.setChapterNumber(chapterNumber);
        chapter.setTitle(dto.getTitle());
        List<ChapterBlockEntity> blocks = dto.getBlocks().stream().map(blockDto -> {
            ChapterBlockEntity block = new ChapterBlockEntity();
            block.setSequenceOrder(blockDto.getSequenceOrder());
            block.setType(blockDto.getType());
            block.setContent(blockDto.getContent());
            block.setChapter(chapter);
            return block;
        }).toList();
        chapter.setBlocks(blocks);
        chapterRepository.save(chapter);
        return ChapterConvertToDto(chapter);
    }
    @Transactional
    @Override
    public ChapterResponseDto updateChapter(Long novelId, Long chapterId, ChapterRequestDto dto){
        novelRepository.findById(novelId).orElseThrow(
                ()->new EntityNotFoundException("Роман с id: " + novelId + " не найден"));

        var chapter= chapterRepository.findById(chapterId).orElseThrow(
                ()->new EntityNotFoundException("глава с ID: " + chapterId + " не найден"));
        chapter.setTitle(dto.getTitle());
        Map<Long, ChapterBlockEntity> existingBlocksMap = chapter.getBlocks().stream()
                .filter(b->b.getId() != null)
                .collect(Collectors.toMap(ChapterBlockEntity::getId, b->b));

        List<ChapterBlockEntity> finalBlocks = new ArrayList<>();
        dto.getBlocks().sort(Comparator.comparing(ChapterBlockRequestDto::getSequenceOrder));
        for(int i =0; i<dto.getBlocks().size(); i++) {
            ChapterBlockRequestDto blockDto =dto.getBlocks().get(i);
            int currentOrder = i+1;
            if(blockDto.getId()!=null && existingBlocksMap.containsKey(blockDto.getId())) {
                ChapterBlockEntity existingBlock = existingBlocksMap.get(blockDto.getId());
                existingBlock.setSequenceOrder(currentOrder);
                existingBlock.setType(blockDto.getType());
                existingBlock.setContent(blockDto.getContent());
                finalBlocks.add(existingBlock);
                existingBlocksMap.remove(blockDto.getId());
            }
            else {
                ChapterBlockEntity newBlock = new ChapterBlockEntity();
                newBlock.setSequenceOrder(currentOrder);
                newBlock.setType(blockDto.getType());
                newBlock.setContent(blockDto.getContent());
                newBlock.setChapter(chapter);
                finalBlocks.add(newBlock);
            }
        }

        chapter.getBlocks().clear();
        chapter.getBlocks().addAll(finalBlocks);

        chapterRepository.save(chapter);
        return ChapterConvertToDto(chapter);
    }

    @Transactional
    @Override
    public void deleteChapter(Long novelId, Long chapterId){
        var novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new EntityNotFoundException("Роман с id: " + novelId + " не найден"));
        ChapterEntity chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new EntityNotFoundException("Глава с id: " + chapterId + " не найден в романе id: " + novelId));
        if(!chapter.getNovel().getId().equals(novelId)) {
            throw new IllegalArgumentException("Глава с id: " + chapterId + "не связана с новеллой с id: " + novelId);
        }
        chapterRepository.delete(chapter);
        novel.setChapterCount(Math.max(0,novel.getChapterCount() - 1));
        novelRepository.save(novel);
    }

    @Override
    @Transactional
    public void updateChapterNumber(Long novelId, List<ChapterOrderUpdateRequestDto> chapterIds){
        novelRepository.findById(novelId).orElseThrow(
                ()->new EntityNotFoundException("Роман с id: " + novelId + " не найден"));
        for(var chapterId : chapterIds){
            chapterRepository.updatePositionSecurely(chapterId.getChapterId(),novelId,chapterId.getNewChapterNumber());
        }
    }

    @Override
    @Transactional
    public ChapterResponseDto findChapter(Long chapterId, Long novelId, Long currentUserId, boolean isLocallyViewed, String guestId) {
        var novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new EntityNotFoundException("Новеллы с id: " + novelId + " нет"));

        if (NON_PUBLIC_STATUSES.contains(novel.getStatus())) {
            if (!Objects.equals(currentUserId, novel.getAuthor().getId())) {
                throw new EntityNotFoundException("Роман не найден.");
            }
        }

        var chapter = chapterRepository.findByNovelIdAndId(novelId, chapterId)
                .orElseThrow(() -> new EntityNotFoundException("Главы не существует"));

        if (!isLocallyViewed) {
            if (currentUserId != null) {
                UserNovelId historyId = new UserNovelId(currentUserId, novel.getId());
                var historyOpt = readingHistoryRepository.findById(historyId);

                if (historyOpt.isEmpty()) {
                    ReadingHistoryEntity newHistory = new ReadingHistoryEntity();
                    newHistory.setId(historyId);
                    newHistory.setUser(userService.getEntityIsActiveAndIsLockedFalse(currentUserId));
                    newHistory.setNovel(novel);
                    newHistory.setLastChapter(chapter);
                    newHistory.setUpdatedAt(OffsetDateTime.now());
                    readingHistoryRepository.save(newHistory);

                    recordUniqueView(novel.getId(), "u:" + currentUserId);
                } else {
                    var history = historyOpt.get();
                    history.setLastChapter(chapter);
                    history.setUpdatedAt(OffsetDateTime.now());
                }
            } else if (guestId != null) {
                recordUniqueView(novel.getId(), "g:" + guestId);
            }
        }
        return ChapterConvertToDto(chapter);
    }

    private void recordUniqueView(Long novelId, String identifier) {
        String hllKey = "novel:views:hll:" + novelId;
        String bufferKey = "novel:views:buffer:" + novelId;

        Long added = redisTemplate.opsForHyperLogLog().add(hllKey, identifier);

        if (added != null && added > 0) {
            redisTemplate.opsForValue().increment(bufferKey);
        }
    }

    @Override
    public NovelEntity getNovelReference(Long id) {
        if (!novelRepository.existsById(id)) throw new EntityNotFoundException("Роман с Id:"+ id +" не найден.");
        return entityManager.getReference(NovelEntity.class, id);
    }
    @Transactional(readOnly = true)
    @Override
    public NovelResponseDto getNovelById(Long novelId) {
        return convertToDto(novelRepository.findById(novelId).orElseThrow(() -> new EntityNotFoundException("Роман с Id:"+ novelId +" не найден.")));
    }
    @Override
    public ChapterEntity getChapterReference(Long id) {
        if(!chapterRepository.existsById(id)) throw new EntityNotFoundException("Глава с Id:"+ id +" не найден.");
        return entityManager.getReference(ChapterEntity.class, id);
    }
    @Override
    public ChapterBlockEntity getBlockReference(Long id) {
        if (!chapterBlockRepository.existsById(id))throw new EntityNotFoundException("Блок с Id:"+ id +" не найден.");
        return entityManager.getReference(ChapterBlockEntity.class, id);
    }
    @Transactional(readOnly = true)
    @Override
    public NovelEntity getNovelEntity(Long id) {
        return novelRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Роман с Id:"+ id +" не найден."));
    }
    @Transactional(readOnly = true)
    @Override
    public ChapterEntity getChapterEntity(Long id) {
        return chapterRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Глава с Id:"+ id +" не найден."));
    }
    @Transactional(readOnly = true)
    @Override
    public ChapterBlockEntity getBlockEntity(Long id) {
        return chapterBlockRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Блок с Id:"+ id +" не найден."));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<NovelResponseDto> searchNovels(int page, int size, String title) {
        Pageable pageable = PageRequest.of(page, size);
        Specification<NovelEntity> spec = NovelSpecifications.titleLike(title);
        return novelRepository.findAll(spec, pageable)
                .map(this::convertToDto);
    }

}