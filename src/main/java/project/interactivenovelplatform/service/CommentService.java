package project.interactivenovelplatform.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.CommentRequestDto;
import project.interactivenovelplatform.dto.request.RatingRequestDto;
import project.interactivenovelplatform.dto.response.AllRatingsResponseDto;
import project.interactivenovelplatform.dto.response.CommentResponseDto;
import project.interactivenovelplatform.dto.response.RatingResponseDto;

import java.util.List;

public interface CommentService {
    RatingResponseDto setRating(Long novelId, Long userId, RatingRequestDto dto);
    AllRatingsResponseDto getRatings(Long novelId, Pageable pageable);
    RatingResponseDto deleteRating(Long novelId,Long ratingId, Long userId);
    CommentResponseDto deleteComment(Long commentId , String userName);
    CommentResponseDto createComment(List<MultipartFile> files, CommentRequestDto dto, Long currentId);

    Slice<CommentResponseDto> getComments(CommentRequestDto filter, Pageable pageable);

}
