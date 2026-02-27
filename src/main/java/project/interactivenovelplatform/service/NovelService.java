package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.*;
import project.interactivenovelplatform.dto.response.ChapterResponseDto;
import project.interactivenovelplatform.dto.response.NovelAndChapterShortResponseDto;
import project.interactivenovelplatform.dto.response.NovelResponseDto;

import java.security.Principal;
import java.util.List;

public interface NovelService {
    NovelResponseDto create(NovelRequestDto dto, Long currentAuthorId);
    NovelAndChapterShortResponseDto findById(Long id, Long userId);
    Page<NovelResponseDto> findAll(NovelSearchRequestDto request, Pageable pageable);
    NovelResponseDto update(Long id, NovelUpdateRequestDto dto);
    NovelResponseDto updateCoverUrl(Long id, @RequestParam("file") MultipartFile file, Principal principal);
    Page<NovelResponseDto> findNewNovels(int page , int size);
    Page<NovelResponseDto> findMyNovels(int page , int size,Long authorId);
    NovelAndChapterShortResponseDto findMyNovel(Long id,Long authorId);
    ChapterResponseDto findChapter(Long chapterId, Long novelId,Long authorId);
    ChapterResponseDto addChapter(Long novelId, ChapterRequestDto dto);
    ChapterResponseDto updateChapter(Long novelId, Long chapterId, ChapterRequestDto dto);
    void deleteChapter(Long novelId, Long chapterId);
    void updateChapterNumber(Long novelId, List<ChapterOrderUpdateRequestDto> chapterIds);

}
