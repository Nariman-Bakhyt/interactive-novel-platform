package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.config.RateLimited;
import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.AllRatingsResponseDto;
import project.interactivenovelplatform.dto.response.RatingResponseDto;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.CommentService;


@RestController
@RequestMapping("/api/rating")
@RequiredArgsConstructor
public class RatingController {
    private final CommentService ratingService;

    @RateLimited(capacity = 5, minutes = 10)
    @PostMapping("/{novelId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RatingResponseDto> setRating(@PathVariable Long novelId, @RequestBody @Valid RatingRequestDto dto, Authentication authentication) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        var body = ratingService.setRating(novelId, user.getId(), dto);
        return ResponseEntity.ok().body(body);
    }
    @RateLimited(capacity = 10, minutes = 1)
    @DeleteMapping("/{novelId}/{ratingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteRating(@PathVariable Long novelId,@PathVariable Long ratingId, Authentication authentication){
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        ratingService.deleteRating(novelId,ratingId, user.getId());
        return ResponseEntity.ok().build();
    }
    @RateLimited(capacity = 30, minutes = 1)
    @GetMapping("/public/{novelId}")
    public ResponseEntity<AllRatingsResponseDto> getRatings(@PathVariable Long novelId,
                                                            @PageableDefault(size = 20, sort = "timestamp",
            direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageSize() > 50) {
            pageable = PageRequest.of(pageable.getPageNumber(), 50, pageable.getSort());
        }
        return ResponseEntity.ok().body(ratingService.getRatings(novelId, pageable));
    }


}