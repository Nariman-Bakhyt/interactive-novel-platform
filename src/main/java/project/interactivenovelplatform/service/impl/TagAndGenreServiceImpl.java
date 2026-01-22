package project.interactivenovelplatform.service.impl;


import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.dto.request.TagOrGenreRequestDto;
import project.interactivenovelplatform.dto.response.TagOrGenreResponseDto;
import project.interactivenovelplatform.entity.GenreEntity;
import project.interactivenovelplatform.entity.NovelEntity;
import project.interactivenovelplatform.entity.TagEntity;
import project.interactivenovelplatform.repository.GenreRepository;
import project.interactivenovelplatform.repository.TagRepository;
import project.interactivenovelplatform.service.TagAndGenreService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TagAndGenreServiceImpl implements TagAndGenreService {
    private final TagRepository tagRepository;
    private final GenreRepository genreRepository;

    @Override
    @Transactional
    public List<TagOrGenreResponseDto> UpdateTagOrGenreToNovel(List<TagOrGenreRequestDto> dto, boolean isTag, NovelEntity novelEntity) {
        if(dto == null)return List.of();
        List<Long> uniqueId =dto.stream().map(TagOrGenreRequestDto::getId).filter(Objects::nonNull).distinct().toList();

        if(isTag){

            List<TagEntity> tags = tagRepository.findAllById(uniqueId);
            novelEntity.getTags().clear();
            novelEntity.getTags().addAll(tags);
            return tags.stream().map(tag->new TagOrGenreResponseDto(tag.getId(),tag.getName())).toList();
        }
        else {
            List<GenreEntity> genres = genreRepository.findAllById(uniqueId);
            novelEntity.getGenres().clear();
            novelEntity.getGenres().addAll(genres);
            return genres.stream().map(g->new TagOrGenreResponseDto(g.getId(),g.getName())).toList();
        }

    }

    @Override
    @Transactional
    public List<TagOrGenreResponseDto> addTagOrGenre(List<TagOrGenreRequestDto> dto,boolean isTag){
        if(dto == null || dto.isEmpty())return List.of();
        List<String> uniqueName = dto.stream().map(TagOrGenreRequestDto::getName)
                .filter(Objects::nonNull)
                .map(name-> name.trim().replaceAll("\\s+"," "))
                .filter(name -> !name.isEmpty())
                .map(name->name.substring(0,1).toUpperCase()+name.substring(1).toLowerCase())
                .distinct().toList();
        List<String> existingName = isTag
            ? tagRepository.findAllByNameInIgnoreCase(uniqueName).stream()
                .map(TagEntity::getName).toList()
            :genreRepository.findAllByNameInIgnoreCase(uniqueName).stream()
                .map(GenreEntity::getName).toList();

        if (isTag) {
            List<TagEntity> tagsToSave = uniqueName.stream()
                    .filter(name -> existingName.stream()
                            .noneMatch(existing -> existing.equalsIgnoreCase(name)))
                    .map(name -> {
                        TagEntity newTag = new TagEntity();
                        newTag.setName(name);
                        return newTag;
                    })
                    .toList();

            if (!tagsToSave.isEmpty()) {
                tagRepository.saveAll(tagsToSave);
            }

            return tagsToSave.stream()
                    .map(tag -> new TagOrGenreResponseDto(tag.getId(), tag.getName()))
                    .toList();
        }
        else {
            List<GenreEntity> genreToSave = uniqueName.stream()
                    .filter(name -> existingName.stream()
                            .noneMatch(existing -> existing.equalsIgnoreCase(name)))
                    .map(
                    name -> {
                        GenreEntity newGenre = new GenreEntity();
                        newGenre.setName(name);
                        return newGenre;
                    }
            ).toList();
            if(!genreToSave.isEmpty()){
                genreRepository.saveAll(genreToSave);
            }
            return genreToSave.stream().map(genre-> new TagOrGenreResponseDto(genre.getId(), genre.getName())).toList();
        }
    }
    @Override
    @Transactional
    public List<TagOrGenreResponseDto> DeleteTagOrGenre(List<TagOrGenreRequestDto> dto,boolean isTag){
        if(dto == null || dto.isEmpty())return List.of();
        List<Long> uniqueId =dto.stream().map(TagOrGenreRequestDto::getId).filter(Objects::nonNull).distinct().toList();
        if(isTag){
            List<TagEntity> tags = tagRepository.findAllById(uniqueId);
            if(tags.isEmpty())return List.of();
            tagRepository.deleteAllById(tags.stream().map(TagEntity::getId).toList());
            return tags.stream().map(tag -> new TagOrGenreResponseDto(tag.getId(), tag.getName())).toList();
        }
        else{
            List<GenreEntity> genres = genreRepository.findAllById(uniqueId);
            if(genres.isEmpty())return List.of();
            genreRepository.deleteAllById(genres.stream().map(GenreEntity::getId).toList());
            return genres.stream().map(genre -> new TagOrGenreResponseDto(genre.getId(), genre.getName())).toList();
        }

    }
    @Override
    public List<TagOrGenreResponseDto> GetAllTagOrGenre(boolean isTag){
        if(isTag){
            return tagRepository.findAll().stream().map(tag-> new TagOrGenreResponseDto(tag.getId(),tag.getName())).toList();
        }
        else{
            return genreRepository.findAll().stream().map(genre -> new TagOrGenreResponseDto(genre.getId(), genre.getName())).toList();
        }
    }
}
