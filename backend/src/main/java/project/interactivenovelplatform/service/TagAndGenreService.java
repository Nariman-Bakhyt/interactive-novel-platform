package project.interactivenovelplatform.service;

import project.interactivenovelplatform.dto.request.TagOrGenreRequestDto;
import project.interactivenovelplatform.dto.response.TagOrGenreResponseDto;
import project.interactivenovelplatform.entity.NovelEntity;

import java.util.List;

public interface TagAndGenreService {
    List<TagOrGenreResponseDto> updateTagOrGenreToNovel(List<Long> ids, boolean isTag, NovelEntity novelEntity);
    List<TagOrGenreResponseDto> addTagOrGenre(List<TagOrGenreRequestDto> dto,boolean isTag);
    List<TagOrGenreResponseDto> deleteTagOrGenre(List<TagOrGenreRequestDto> dto,boolean isTag);
    List<TagOrGenreResponseDto> getAllTagOrGenre(boolean isTag);

}
