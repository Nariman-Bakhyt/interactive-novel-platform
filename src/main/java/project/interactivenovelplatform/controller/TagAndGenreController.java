package project.interactivenovelplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.config.RateLimited;
import project.interactivenovelplatform.dto.request.TagOrGenreRequestDto;
import project.interactivenovelplatform.dto.response.TagOrGenreResponseDto;
import project.interactivenovelplatform.service.TagAndGenreService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TagAndGenreController {
    private final TagAndGenreService tagAndGenreService;

    @RateLimited(capacity = 5, minutes = 60)
    @GetMapping("/genres/public")
    public ResponseEntity<List<TagOrGenreResponseDto>> getAllGenre(){
        return ResponseEntity.ok().body(tagAndGenreService.GetAllTagOrGenre(false));
    }
    @RateLimited(capacity = 5, minutes = 60)
    @GetMapping("/tags/public")
    public ResponseEntity<List<TagOrGenreResponseDto>> getAllTags() {
        return ResponseEntity.ok().body(tagAndGenreService.GetAllTagOrGenre(true));
    }
    @RateLimited(capacity = 5, minutes = 1)
    @PutMapping("/genres")
    @PreAuthorize("@rsec.hasRank(T(project.interactivenovelplatform.entity.Role).SUPER_ADMIN)")
    public ResponseEntity<List<TagOrGenreResponseDto>> addGenre(@RequestBody List<TagOrGenreRequestDto> dto) {
        return ResponseEntity.ok().body(tagAndGenreService.addTagOrGenre(dto,false));
    }
    @RateLimited(capacity = 5, minutes = 1)
    @PutMapping("/tags")
    @PreAuthorize("@rsec.hasRank(T(project.interactivenovelplatform.entity.Role).SUPER_ADMIN)")
    public ResponseEntity<List<TagOrGenreResponseDto>> addTag(@RequestBody List<TagOrGenreRequestDto> dto) {
        return ResponseEntity.ok().body(tagAndGenreService.addTagOrGenre(dto,true));
    }

}
