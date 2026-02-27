package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.AllRatingResponseDto;
import project.interactivenovelplatform.dto.response.AllRatingsResponseDto;
import project.interactivenovelplatform.dto.response.RatingResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.service.CommentService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rating")
@RequiredArgsConstructor
public class RatingController {
    private final CommentService ratingService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/{novelId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RatingResponseDto> setRating(@PathVariable Long novelId, @RequestBody @Valid RatingRequestDto dto, Authentication authentication) {
        AppUserEntity user = (AppUserEntity) authentication.getPrincipal();
        var body = ratingService.setRating(novelId, user.getId(), dto);
        messagingTemplate.convertAndSend("/topic/novel." + novelId + ".ratings", body);
        return ResponseEntity.ok().body(body);
    }

    @DeleteMapping("/{novelId}/{ratingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteRating(@PathVariable Long novelId,@PathVariable Long ratingId, Authentication authentication){
        AppUserEntity user = (AppUserEntity) authentication.getPrincipal();
        var body = ratingService.deleteRating(novelId,ratingId, user.getId());

        String destination = "/topic/novel." + novelId + ".ratings";
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", body.getRatingId());
        payload.put("deleted", true);
        payload.put("score", body.getScore());
        messagingTemplate.convertAndSend(destination, payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/public/{novelId}")
    public ResponseEntity<AllRatingsResponseDto> getRatings(@PathVariable Long novelId,
                                                            @PageableDefault(size = 20, sort = "timestamp",
            direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok().body(ratingService.getRatings(novelId, pageable));
    }


}
