package project.interactivenovelplatform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.config.RateLimited;
import project.interactivenovelplatform.dto.request.*;
import project.interactivenovelplatform.dto.response.ChapterResponseDto;
import project.interactivenovelplatform.dto.response.NovelAndChapterShortResponseDto;
import project.interactivenovelplatform.dto.response.NovelResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.NovelService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
public class NovelController {
    private final NovelService novelService;

    @RateLimited(capacity = 15, minutes = 1)
    @PostMapping("/public")
    public ResponseEntity<PagedModel<NovelResponseDto>> findAllNovels(@RequestBody NovelSearchRequestDto dto
            , @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size
            ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<NovelResponseDto> novels = novelService.findAll(dto,pageable);
        return ResponseEntity.ok(new PagedModel<>(novels));
    }
    @RateLimited(capacity = 15, minutes = 1)
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
    @RateLimited(capacity = 15, minutes = 1)
    @GetMapping("/my/{novelId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NovelAndChapterShortResponseDto> findMyNovelById(@PathVariable Long novelId,Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserPrincipal userDetails) {
            var currentAuthorId = userDetails.getId();
            NovelAndChapterShortResponseDto novel = novelService.findMyNovel(novelId, currentAuthorId);
            return ResponseEntity.ok(novel);
        }
        throw new org.springframework.security.authentication.BadCredentialsException("Не удалось получить данные пользователя");

    }

    @RateLimited(capacity = 5, minutes = 10)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NovelResponseDto> createNovel(
            @RequestBody @Valid NovelRequestDto novelRequestDto,
            Authentication authentication
    ) {
        if (authentication.getPrincipal() instanceof UserPrincipal userDetails) {
            var currentAuthorId = userDetails.getId();
            NovelResponseDto createdNovel = novelService.create(novelRequestDto, currentAuthorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdNovel);
        }
        throw new org.springframework.security.authentication.BadCredentialsException("Не удалось получить данные пользователя");
    }
    @RateLimited(capacity = 5, minutes = 10)
    @PutMapping("/{novelId}")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId, authentication.name) or hasRole('ADMIN')")
    public ResponseEntity<NovelResponseDto> updateNovel(
            @PathVariable Long novelId,
            @RequestBody NovelUpdateRequestDto novelRequestDto
    ) {
        NovelResponseDto updatedNovel = novelService.update(novelId, novelRequestDto);
        return ResponseEntity.ok(updatedNovel);
    }
    @RateLimited(capacity = 5, minutes = 60)
    @PostMapping("{novelId}/cover")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId, authentication.name) or @rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN)")
    public ResponseEntity<NovelResponseDto> updateCover(@RequestParam(value = "file", required = false) MultipartFile file,@PathVariable Long novelId , Principal principal){
        var novel = novelService.updateCoverUrl(novelId,file,principal);
        return ResponseEntity.ok().body(novel);
    }

    @RateLimited(capacity = 10, minutes = 1)
    @GetMapping("/public/new")
    public ResponseEntity<PagedModel<NovelResponseDto>> findAllNewNovels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        Page<NovelResponseDto>  novels = novelService.findNewNovels(page, size) ;
        return ResponseEntity.ok().body(new PagedModel<>(novels));
    }
    @RateLimited(capacity = 10, minutes = 1)
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedModel<NovelResponseDto>> findMyNovels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
            ){

        if (authentication.getPrincipal() instanceof UserPrincipal userDetails) {
            var currentAuthorId = userDetails.getId();
            Page<NovelResponseDto>  novels =novelService.findMyNovels(page, size,currentAuthorId);
            return ResponseEntity.ok().body(new PagedModel<>(novels));
        }
        throw new org.springframework.security.authentication.BadCredentialsException("Не удалось получить данные пользователя");
    }
    @RateLimited(capacity = 3, minutes = 1)
    @PostMapping("/{novelId}/addchapter")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId,authentication.name)or @rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN) ")
    public ResponseEntity<ChapterResponseDto> addChapter(@PathVariable Long novelId,
                                                         @Valid @RequestBody ChapterRequestDto dto
    ){
        var chapter = novelService.addChapter(novelId,dto);
        return ResponseEntity.ok().body(chapter);
    }
    @RateLimited(capacity = 30, minutes = 1)
    @GetMapping("/public/{novelId}/chapter/{chapterId}")
    public ResponseEntity<ChapterResponseDto> findChapter(
            @PathVariable Long novelId,
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "false") boolean isLocallyViewed,
            HttpServletRequest request,
            Authentication authentication) {
        Long currentUserId = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            currentUserId = principal.getId();
        }
        String guestId = (String) request.getAttribute("VALID_GUEST_ID");

        var chapter = novelService.findChapter(chapterId, novelId, currentUserId, isLocallyViewed, guestId);
        return ResponseEntity.ok().body(chapter);
    }
    @RateLimited(capacity = 5, minutes = 1)
    @PutMapping("/{novelId}/updatechapter/{chapterId}")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId,authentication.name)or @rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN) ")
    public ResponseEntity<ChapterResponseDto> updateChapter(@PathVariable Long novelId, @Valid @RequestBody ChapterRequestDto dto,@PathVariable Long chapterId){
        var chapter =  novelService.updateChapter(novelId,chapterId,dto);
        return ResponseEntity.ok().body(chapter);
    }
    @RateLimited(capacity = 5, minutes = 5)
    @PostMapping("/{novelId}/updatenumeric")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId,authentication.name)or @rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN) ")
    public ResponseEntity<?> updateChapterNumber(@PathVariable Long novelId, @RequestBody List<ChapterOrderUpdateRequestDto> dtos){
        novelService.updateChapterNumber(novelId,dtos);
        return ResponseEntity.ok().build();
    }
    @RateLimited(capacity = 5, minutes = 10)
    @DeleteMapping("/{novelId}/chapter/{chapterId}")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId,authentication.name)or @rsec.hasRank(T(project.interactivenovelplatform.entity.Role).ADMIN) ")
    public ResponseEntity<?> deleteChapter (@PathVariable Long novelId , @PathVariable Long chapterId)  {
        novelService.deleteChapter(novelId,chapterId);
        return ResponseEntity.ok().build();
    }
    @RateLimited(capacity = 40, minutes = 1)
    @GetMapping("/public/search")
    public ResponseEntity<PagedModel<NovelResponseDto>> searchNovels(
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Page<NovelResponseDto> body = novelService.searchNovels(page, size, title);
        return ResponseEntity.ok(new PagedModel<>(body));
    }



//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> deleteNovel(@PathVariable Long id) {
//        novelService.delete(id);
//        return ResponseEntity.noContent().build();
//    }
}
