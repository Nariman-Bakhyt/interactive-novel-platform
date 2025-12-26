package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.NovelRequestDto;
import project.interactivenovelplatform.dto.request.NovelUpdateRequestDto;
import project.interactivenovelplatform.dto.response.NovelResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.service.NovelService;
import project.interactivenovelplatform.service.UserService;

import java.security.Principal;

@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
public class NovelController {
    private final NovelService novelService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<NovelResponseDto>> findAllNovels(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NovelResponseDto> novels = novelService.findAll(pageable);
        return ResponseEntity.ok(novels);
    }
    @GetMapping("/{novelId}")
    public ResponseEntity<NovelResponseDto> findNovelById(@PathVariable Long novelId) {
        NovelResponseDto novel = novelService.findById(novelId);
        return ResponseEntity.ok(novel);
    }
    @GetMapping("/my/{novelId}")
    public ResponseEntity<NovelResponseDto> findMyNovelById(@PathVariable Long novelId,Authentication authentication) {
        var currentAuthorId = userService.findByUsername(authentication.getName()).getId();
        NovelResponseDto novel = novelService.findMyNovel(novelId, currentAuthorId);
        return ResponseEntity.ok(novel);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NovelResponseDto> createNovel(
            @RequestBody @Valid NovelRequestDto novelRequestDto,
            Authentication authentication
    ) {
        var currentAuthorId = userService.findByUsername(authentication.getName()).getId();
        NovelResponseDto createdNovel = novelService.create(novelRequestDto, currentAuthorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNovel);
    }

    @PutMapping("/{novelId}")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId, authentication.name) or hasRole('ADMIN')")
    public ResponseEntity<NovelResponseDto> updateNovel(
            @PathVariable Long novelId,
            @RequestBody NovelUpdateRequestDto novelRequestDto,
            Authentication authentication
    ) {
        var currentAuthorId = userService.findByUsername(authentication.getName()).getId();
        NovelResponseDto updatedNovel = novelService.update(novelId, novelRequestDto, currentAuthorId);
        return ResponseEntity.ok(updatedNovel);
    }

    @PostMapping("{novelId}/cover")
    @PreAuthorize("@novelServiceImpl.isAuthor(#novelId, authentication.name) or hasAnyRole('ADMIN')")
    public ResponseEntity<NovelResponseDto> updateCover(@RequestParam("file") MultipartFile file,@PathVariable Long novelId , Principal principal){
        var novel = novelService.updateCoverUrl(novelId,file,principal);
        return ResponseEntity.ok().body(novel);
    }

    @GetMapping("/new")
    public ResponseEntity<Page<NovelResponseDto>> findAllNewNovels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        return ResponseEntity.ok().body(novelService.findNewNovels(page, size));
    }
    @GetMapping("/my")
    public ResponseEntity<Page<NovelResponseDto>> findMyNovels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
            ){
        var currentAuthorId = userService.findByUsername(authentication.getName()).getId();
        return ResponseEntity.ok().body(novelService.findMyNovels(page, size,currentAuthorId));
    }

//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> deleteNovel(@PathVariable Long id) {
//        novelService.delete(id);
//        return ResponseEntity.noContent().build();
//    }
}
