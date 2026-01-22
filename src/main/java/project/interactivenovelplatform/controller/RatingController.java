package project.interactivenovelplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.RatingStatsDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.service.RatingService;

@RestController
@RequestMapping("/api/rating")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;


    @PostMapping("/{novelId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RatingStatsDto> setRating(@PathVariable Long novelId, @RequestBody RatingRequestDto dto, Authentication authentication) {
        AppUserEntity user = (AppUserEntity) authentication.getPrincipal();
        var body = ratingService.setRating(novelId, user.getId(), dto);
        return ResponseEntity.ok().body(body);
    }

    @DeleteMapping("/{novelId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RatingStatsDto> deleteRating(@PathVariable Long novelId, Authentication authentication){
        AppUserEntity user = (AppUserEntity) authentication.getPrincipal();
        var body = ratingService.deleteRating(novelId, user.getId());
        return ResponseEntity.ok().body(body);
    }
}
