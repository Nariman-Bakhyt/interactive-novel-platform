package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.NovelRequestDto;
import project.interactivenovelplatform.dto.request.NovelUpdateRequestDto;
import project.interactivenovelplatform.dto.response.NovelResponseDto;
import project.interactivenovelplatform.entity.Novel;

import java.security.Principal;

public interface NovelService {
    NovelResponseDto create(NovelRequestDto dto, Long currentAuthorId);
    NovelResponseDto findById(Long id);
    Page<NovelResponseDto> findAll(Pageable pageable);
    NovelResponseDto update(Long id, NovelUpdateRequestDto dto, Long currentAuthorId);
    NovelResponseDto updateCoverUrl(Long id, @RequestParam("file") MultipartFile file, Principal principal);
    Page<NovelResponseDto> findNewNovels(int page , int size);
    Page<NovelResponseDto> findMyNovels(int page , int size,Long authorId);
    NovelResponseDto findMyNovel(Long id,Long authorId);
}
