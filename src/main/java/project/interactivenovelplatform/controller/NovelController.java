package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.*;
import project.interactivenovelplatform.dto.response.ChapterResponseDto;
import project.interactivenovelplatform.dto.response.NovelAndChapterShortResponseDto;
import project.interactivenovelplatform.dto.response.NovelResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.service.NovelService;
import project.interactivenovelplatform.service.UserService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
public class NovelController {
    private final NovelService novelService;
    private final UserService userService;

    @PostMapping("/public")
    public ResponseEntity<Page<NovelResponseDto>> findAllNovels(@RequestBody NovelSearchRequestDto dto
            ,@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "20") int size
            ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NovelResponseDto> novels = novelService.findAll(dto,pageable);
        return ResponseEntity.ok(novels);
    }
    @GetMapping("/public/{novelId}")
    public ResponseEntity<NovelAndChapterShortResponseDto> findNovelById(@PathVariable Long novelId,
                                                                         @AuthenticationPrincipal AppUserEntity user) {
        NovelAndChapterShortResponseDto novel;
        if(user != null) {
            novel = novelService.findById(novelId,user.getId());
        }
        else {
            novel = novelService.findById(novelId,null);
        }

        return ResponseEntity.ok(novel);
    }
    @GetMapping("/my/{novelId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NovelAndChapterShortResponseDto> findMyNovelById(@PathVariable Long novelId,Authentication authentication) {
        var principal = (AppUserEntity) authentication.getPrincipal();
        var currentAuthorId = principal.getId();
        NovelAndChapterShortResponseDto novel = novelService.findMyNovel(novelId, currentAuthorId);
        return ResponseEntity.ok(novel);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NovelResponseDto> createNovel(
            @RequestBody @Valid NovelRequestDto novelRequestDto,
            Authentication authentication
    ) {
        var principal = (AppUserEntity) authentication.getPrincipal();
        var currentAuthorId = principal.getId();
        NovelResponseDto createdNovel = novelService.create(novelRequestDto, currentAuthorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNovel);
    }

    @PutMapping("/{novelId}")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId, authentication.name) or hasRole('ADMIN')")
    public ResponseEntity<NovelResponseDto> updateNovel(
            @PathVariable Long novelId,
            @RequestBody NovelUpdateRequestDto novelRequestDto
    ) {
        NovelResponseDto updatedNovel = novelService.update(novelId, novelRequestDto);
        return ResponseEntity.ok(updatedNovel);
    }

    @PostMapping("{novelId}/cover")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId, authentication.name) or @rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN)")
    public ResponseEntity<NovelResponseDto> updateCover(@RequestParam(value = "file", required = false) MultipartFile file,@PathVariable Long novelId , Principal principal){
        var novel = novelService.updateCoverUrl(novelId,file,principal);
        return ResponseEntity.ok().body(novel);
    }

    @GetMapping("/public/new")
    public ResponseEntity<Page<NovelResponseDto>> findAllNewNovels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok().body(novelService.findNewNovels(page, size));
    }
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NovelResponseDto>> findMyNovels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
            ){
        var principal = (AppUserEntity) authentication.getPrincipal();
        var currentAuthorId = principal.getId();
        return ResponseEntity.ok().body(novelService.findMyNovels(page, size,currentAuthorId));
    }
    @PostMapping("/{novelId}/addchapter")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId,authentication.name)or @rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN) ")
    public ResponseEntity<ChapterResponseDto> addChapter(@PathVariable Long novelId,
                                                         @Valid @RequestBody ChapterRequestDto dto
    ){
        var chapter = novelService.addChapter(novelId,dto);
        return ResponseEntity.ok().body(chapter);
    }
    @GetMapping("/public/{novelId}/chapter/{chapterId}")
    public ResponseEntity<ChapterResponseDto> findChapter(@PathVariable Long novelId,
                                                          @PathVariable Long chapterId,
                                                          @AuthenticationPrincipal AppUserEntity user)
    {
        Long currentUserId = (user != null) ? user.getId() : null;
        var chapter = novelService.findChapter(chapterId,novelId,currentUserId);
        return ResponseEntity.ok().body(chapter);
    }
    @PutMapping("/{novelId}/updatechapter/{chapterId}")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId,authentication.name)or @rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN) ")
    public ResponseEntity<ChapterResponseDto> updateChapter(@PathVariable Long novelId, @Valid @RequestBody ChapterRequestDto dto,@PathVariable Long chapterId){
        var chapter =  novelService.updateChapter(novelId,chapterId,dto);
        return ResponseEntity.ok().body(chapter);
    }

    @PostMapping("/{novelId}/updatenumeric")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId,authentication.name)or @rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN) ")
    public ResponseEntity<?> updateChapterNumber(@PathVariable Long novelId, @RequestBody List<ChapterOrderUpdateRequestDto> dtos){
        novelService.updateChapterNumber(novelId,dtos);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{novelId}/chapter/{chapterId}")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId,authentication.name)or @rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN) ")
    public ResponseEntity<?> deleteChapter (@PathVariable Long novelId , @PathVariable Long chapterId)  {
        novelService.deleteChapter(novelId,chapterId);
        return ResponseEntity.ok().build();
    }



//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> deleteNovel(@PathVariable Long id) {
//        novelService.delete(id);
//        return ResponseEntity.noContent().build();
//    }
}
