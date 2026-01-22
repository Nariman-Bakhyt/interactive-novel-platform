package project.interactivenovelplatform.service;

import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.RatingStatsDto;

public interface RatingService {
    RatingStatsDto setRating(Long novelId, Long userId, RatingRequestDto dto);
    RatingStatsDto  deleteRating(Long novelId, Long userId);
}
