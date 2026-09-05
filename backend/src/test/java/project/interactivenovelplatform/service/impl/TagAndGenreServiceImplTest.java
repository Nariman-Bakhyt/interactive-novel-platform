package project.interactivenovelplatform.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.interactivenovelplatform.dto.request.TagOrGenreRequestDto;
import project.interactivenovelplatform.dto.response.TagOrGenreResponseDto;
import project.interactivenovelplatform.entity.NovelEntity;
import project.interactivenovelplatform.entity.TagEntity;
import project.interactivenovelplatform.repository.GenreRepository;
import project.interactivenovelplatform.repository.TagRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagAndGenreServiceImplTest {
    @Mock
    private TagRepository tagRepository;
    @Mock
    private GenreRepository genreRepository;
    @InjectMocks
    private TagAndGenreServiceImpl tagAndGenreServiceImpl;



    @Test
    @DisplayName("Если передать null List.of")
    void addTagOrGenreNull() {
        assertEquals(tagAndGenreServiceImpl.addTagOrGenre(List.of(),false),List.of());
        assertEquals(tagAndGenreServiceImpl.addTagOrGenre(null,false),List.of());
    }

    @Test
    @DisplayName("addTagOrGenre: Успешное форматирование строк и сохранение новых тегов")
    void addTagOrGenrePositive() {
        // 1. Arrange (Подготовка)
        // Передаем "кривые" строки с пробелами и разным регистром
        List<TagOrGenreRequestDto> requestDtos = List.of(
                new TagOrGenreRequestDto(1L, "  фЭнТеЗи  "),
                new TagOrGenreRequestDto(2L, "романтика")
        );

        List<String> expectedFormattedNames = List.of("Фэнтези", "Романтика");
        // Говорим моку: если сервис запросит поиск "Фэнтези" и "Романтика", вернуть пустой список (в БД их пока нет)
        when(tagRepository.findAllByNameInIgnoreCase(expectedFormattedNames))
                .thenReturn(List.of());
        // 2. Act (Вызов тестируемого метода)
        List<TagOrGenreResponseDto> result = tagAndGenreServiceImpl.addTagOrGenre(requestDtos, true);
        // 3. Assert (Проверка результата)
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(TagOrGenreResponseDto::getName).toList())
                .containsExactly("Фэнтези", "Романтика");

        // 4. Verify + ArgumentCaptor: проверяем, что в репозиторий ушли сущности с правильными именами
        ArgumentCaptor<List<TagEntity>> captor = ArgumentCaptor.forClass(List.class);

        verify(tagRepository, times(1)).saveAll(captor.capture());

        List<TagEntity> savedEntities = captor.getValue();

        assertThat(savedEntities)
                .extracting(TagEntity::getName)
                .containsExactly("Фэнтези", "Романтика");
    }

    @Test
    @DisplayName("addTagOrGenre: Игнорирование уже существующих в БД тегов и сохранение только новых")
    void addTagOrGenreNegative() {
        List<TagOrGenreRequestDto> requestDtos = List.of(
                new TagOrGenreRequestDto(1L, "  фЭнТеЗи  "),
                new TagOrGenreRequestDto(2L, " МаГия ")
        );
        List<String> expectedFormattedNames = List.of("Фэнтези", "Магия");
        when(tagRepository.findAllByNameInIgnoreCase(expectedFormattedNames)).thenReturn(List.of(new TagEntity(1L,"Фэнтези")));

        List<TagOrGenreResponseDto> result = tagAndGenreServiceImpl.addTagOrGenre(requestDtos, true);

        assertThat(result).hasSize(1);
        assertThat(result.stream().map(TagOrGenreResponseDto::getName).toList())
                .containsExactly("Магия");

        ArgumentCaptor<List<TagEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(tagRepository,times(1)).saveAll(captor.capture());
        List<TagEntity> savedEntities = captor.getValue();
        assertThat(savedEntities).extracting(TagEntity::getName).containsExactly("Магия");
    }


    @Test
    void deleteTagOrGenrePositive() {
        List<TagOrGenreRequestDto> requestDtos = List.of(
                new TagOrGenreRequestDto(2L,"Зомби")
                ,new TagOrGenreRequestDto(3L,"гг мужчина"));
        List<Long> expectedIds = List.of(2L,3L);
        when(tagRepository.findAllById(expectedIds)).thenReturn(List.of(
                new TagEntity(2L,"Зомби"),
                new TagEntity(3L,"гг мужчина")));

        List<TagOrGenreResponseDto> result = tagAndGenreServiceImpl.deleteTagOrGenre(requestDtos, true);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TagOrGenreResponseDto::getId).containsExactly(2L,3L);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);

        verify(tagRepository,times(1)).deleteAllById(captor.capture());
        List<Long> savedIds = captor.getValue();
        assertThat(savedIds).hasSize(2).containsExactly(2L,3L);
    }

    @Test
    @DisplayName("DeleteTagOrGenre: Когда теги с указанными ID не найдены в БД, ничего не удаляется")
    void deleteTagOrGenreNegative() {
        List<TagOrGenreRequestDto> requestDtos = List.of(
                new TagOrGenreRequestDto(2L,"Зомби")
                ,new TagOrGenreRequestDto(3L,"гг мужчина"));
        List<Long> expectedIds = List.of(2L,3L);

        when(tagRepository.findAllById(expectedIds)).thenReturn(List.of());

        List<TagOrGenreResponseDto> result = tagAndGenreServiceImpl.deleteTagOrGenre(requestDtos, true);
        assertThat(result).hasSize(0);
        assertThat(result).extracting(TagOrGenreResponseDto::getId).containsExactly();


        verify(tagRepository, never()).deleteAllById(any());
    }


    @Test
    @DisplayName("UpdateTagOrGenreToNovel: Успешное обновление тегов в новелле")
    void updateTagOrGenreToNovelPositive() {
        NovelEntity novelEntity = new NovelEntity();
        novelEntity.setTags(new HashSet<>(Set.of(new TagEntity(1L,"Зомби"),
                new TagEntity(2L,"гг мужчина"),
                new TagEntity(3L,"Выживание"))));
        List<Long> expectedIds = List.of(1L,2L,4L);
        when(tagRepository.findAllById(expectedIds)).thenReturn(List.of(new TagEntity(1L,"Зомби"),
                new TagEntity(2L,"гг мужчина"),
                new TagEntity(4L,"Апокалипсис")));

        List<TagOrGenreResponseDto> result = tagAndGenreServiceImpl
                .updateTagOrGenreToNovel(expectedIds,true,novelEntity);
        assertThat(result).hasSize(3);
        assertThat(result).extracting(TagOrGenreResponseDto::getName)
                .containsExactly("Зомби","гг мужчина","Апокалипсис");
        assertThat(novelEntity.getTags()).extracting(TagEntity::getName)
                .containsExactlyInAnyOrder("Зомби","гг мужчина","Апокалипсис");

        verifyNoInteractions(genreRepository);

    }


    @Test
    void updateTagOrGenreToNovelListOfNull() {
        List<Long> expectedIds = List.of();
        NovelEntity novelEntity = new NovelEntity();
        novelEntity.setTags(new HashSet<>(Set.of(new TagEntity(1L,"Зомби"),
                new TagEntity(2L,"гг мужчина"),
                new TagEntity(3L,"Выживание"))));

        List<TagOrGenreResponseDto> result = tagAndGenreServiceImpl
                .updateTagOrGenreToNovel(expectedIds,true,novelEntity);
        assertThat(result).hasSize(0);
        assertEquals(result,List.of());
        assertThat(novelEntity.getTags()).isEmpty();
    }

    @Test
    void updateTagOrGenreToNovelNull() {
        List<Long> expectedIds = null;
        NovelEntity novelEntity = new NovelEntity();
        novelEntity.setTags(new HashSet<>(Set.of(new TagEntity(1L,"Зомби"),
                new TagEntity(2L,"гг мужчина"),
                new TagEntity(3L,"Выживание"))));

        List<TagOrGenreResponseDto> result = tagAndGenreServiceImpl
                .updateTagOrGenreToNovel(expectedIds,true,novelEntity);
        assertThat(result).hasSize(0);
        assertEquals(result,List.of());
        assertThat(novelEntity.getTags()).hasSize(3);
    }

    @Test
    void getAllTagOrGenre() {
        List<TagEntity> expectedTags = List.of(new TagEntity(1L,"Зомби"),
                new TagEntity(2L,"гг мужчина"),
                new TagEntity(3L,"Выживание"));

        when(tagRepository.findAll()).thenReturn(expectedTags);

        List<TagOrGenreResponseDto> result = tagAndGenreServiceImpl.getAllTagOrGenre(true);
        assertThat(result).hasSize(3)
                .extracting(TagOrGenreResponseDto::getId,TagOrGenreResponseDto::getName)
                .containsExactlyInAnyOrder(tuple(1L,"Зомби"),tuple(2L,"гг мужчина"),tuple(3L,"Выживание"));
        verifyNoInteractions(genreRepository);
    }
}