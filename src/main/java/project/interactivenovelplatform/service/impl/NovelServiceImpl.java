package project.interactivenovelplatform.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.NovelRequestDto;
import project.interactivenovelplatform.dto.request.NovelUpdateRequestDto;
import project.interactivenovelplatform.dto.response.NovelResponseDto;
import project.interactivenovelplatform.entity.Novel;
import project.interactivenovelplatform.entity.NovelEntity;
import project.interactivenovelplatform.repository.NovelRepository;
import project.interactivenovelplatform.service.NovelService;
import project.interactivenovelplatform.service.RoleService;
import project.interactivenovelplatform.service.StorageService;
import project.interactivenovelplatform.service.UserService;

import java.security.Principal;
import java.util.Collection;
import java.util.List;


@AllArgsConstructor
@Service
public class NovelServiceImpl implements NovelService {
    private final UserService userService;
    private final RoleService roleService;
    private final StorageService storageService;
    private final NovelRepository novelRepository;
    private static final Collection<Novel> NON_PUBLIC_STATUSES =
            List.of(Novel.DRAFT, Novel.ARCHIVED, Novel.RETRACTED);

    private NovelResponseDto convertToDto(NovelEntity novel) {
        return new NovelResponseDto(
                novel.getId(),
                novel.getTitle(),
                novel.getStatus().toString(),
                novel.getDescription(),
                novel.getPublicationDate(),
                novel.getChapterCount(),
                novel.getAverageRating(),
                novel.getRatingCount(),
                novel.getViewCount(),
                novel.getAuthor().getUsername(),
                novel.getCoverUrl()
        );
    }
    private Novel getStatusFromString(String statusName) {
        try {
            return Novel.valueOf(statusName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неверный статус: " + statusName);
        }
    }
    private NovelEntity findNovelAndCheckAuthor(Long novelId, Long expectedAuthorId) {
        NovelEntity novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new EntityNotFoundException("Роман с id: " + novelId + " не найден"));

        if (!novel.getAuthor().getId().equals(expectedAuthorId)) {
            throw new AccessDeniedException("Вы не являетесь автором этого романа.");
        }
        return novel;
    }
    public boolean isAuthor(Long novelId, String username) {
        return novelRepository.findById(novelId)
                .map(novel ->
                        novel.getAuthor().getUsername().equals(username)
                )
                .orElse(false);
    }

    @Override
    @Transactional
    public NovelResponseDto create(NovelRequestDto dto, Long currentAuthorId){
        var author = userService.findById(currentAuthorId);
        NovelEntity novelEntity = new NovelEntity();
        novelEntity.setTitle(dto.getTitle());
        novelEntity.setDescription(dto.getDescription());
        novelEntity.setAuthor(userService.getAuthorReference(currentAuthorId));
        novelEntity.setStatus(Novel.DRAFT);
        return convertToDto(novelRepository.save(novelEntity));
    }
    @Override
    @Transactional
    public NovelResponseDto findById(Long id) {
        var novel = novelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Роман не найден."));
        if(NON_PUBLIC_STATUSES.contains(novel.getStatus())) {
            throw new EntityNotFoundException("Роман не найден.");
        }
        novelRepository.incrementViewCount(id);
        return convertToDto(novel);
    }
    @Override
    public Page<NovelResponseDto> findAll(Pageable pageable) {
        // Используем findByStatusNotIn для исключения DRAFT, ARCHIVED и RETRACTED
        Page<NovelEntity> novelPage = novelRepository.findByStatusNotIn(NON_PUBLIC_STATUSES, pageable);

        return novelPage.map(this::convertToDto);
    }
    @Override
    @Transactional
    public NovelResponseDto update(Long id, NovelUpdateRequestDto dto, Long currentAuthorId) {
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
            if(novel.getStatus() == Novel.RETRACTED) {
                throw new IllegalStateException("Статус отозванного романа изменить нельзя.");
            }
            Novel newStatus = getStatusFromString(dto.getStatus());
            novel.setStatus(newStatus);
        }
        return convertToDto(novelRepository.save(novel));
    }


    @Override
    public NovelResponseDto updateCoverUrl(Long id, @RequestParam("file") MultipartFile file, Principal principal){
        try {
            var novel = novelRepository.findById(id)
                    .orElseThrow(()->new EntityNotFoundException("новелла с ID: " + id + " не найден"));
            if(novel.getCoverUrl()!=null) {
                storageService.deleteFile(novel.getCoverUrl());
                novel.setCoverUrl(null);
            }
            if (file.isEmpty()){
                novelRepository.save(novel);
                return convertToDto(novel);
            }

            String fileExtension = storageService.getFileExtension(file.getOriginalFilename());
            if (!storageService.getAllowedExtensions().contains(fileExtension)) {
                throw new BadRequestException("Недопустимый формат файла. Разрешены только JPG, PNG, GIF.");
            }
            Long userId = Long.parseLong(principal.getName());
            String filename = "user_" + userId + id + fileExtension;
            String newCoverUrl = storageService.uploadFile(file,"Cover", filename);
            novel.setCoverUrl(newCoverUrl);
            novelRepository.save(novel);
            return convertToDto(novel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public Page<NovelResponseDto> findNewNovels(int page , int size){
        Pageable pageable = PageRequest.of(page, size);
        return novelRepository.findAllByStatusNotInOrderByPublicationDateDesc(NON_PUBLIC_STATUSES,pageable).map(this::convertToDto);
    }
    @Override
    public Page<NovelResponseDto> findMyNovels(int page , int size,Long authorId){
        Pageable pageable = PageRequest.of(page, size);
        return novelRepository.findAllByAuthor_Id(authorId, pageable).map(this::convertToDto);
    }
    @Override
    public NovelResponseDto findMyNovel(Long id,Long authorId){
        var novel = novelRepository.findByAuthor_IdAndId(authorId,id).orElseThrow(()->new EntityNotFoundException("новелла с ID: " + id + " не найден"));
        return convertToDto(novel);
    }
}
