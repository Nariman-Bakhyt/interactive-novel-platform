package project.interactivenovelplatform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import project.interactivenovelplatform.dto.request.NovelRequestDto;
import project.interactivenovelplatform.dto.response.NovelResponseDto;
import project.interactivenovelplatform.entity.Novel;

public interface NovelService {
    NovelResponseDto create(NovelRequestDto dto, Long currentAuthorId);
    NovelResponseDto findById(Long id);
    Page<NovelResponseDto> findAll(Pageable pageable);
    NovelResponseDto update(Long id, NovelRequestDto dto, Long currentAuthorId);
    NovelResponseDto changeStatus(Long id, Long currentAuthorId, Novel newStatus);
}
