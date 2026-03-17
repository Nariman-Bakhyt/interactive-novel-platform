export interface RatingRequestDto{
  score: number;
  commentText: string;

}
export interface RatingResponseDto {
  ratingId: number;
  totalScore: number;
  ratingCount: number;
  averageRating: number;
  content?:string;
  username?:string;
  timestamp?:string;
  score?:number;
}
export interface AllRatingResponseDto {
  ratingId:number;
  content:string;
  username:string;
  timestamp:string;
  score:number;
}
export interface AllRatingsResponseDto {
  totalScore: number;
  ratingCount: number;
  averageRating: number;
  allRatings: {
    content: AllRatingResponseDto[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    last: boolean;
  };
}
