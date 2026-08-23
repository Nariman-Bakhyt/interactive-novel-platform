package project.interactivenovelplatform.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter@Setter
@AllArgsConstructor
public class UserRelationRequestDto {
    @NotNull
    @Min(1)
    private Long relationId;
    @NotNull(message = "ID получателя не может быть пустым")
    @Min(value = 1, message = "ID должен быть больше 0")
    private Long receiverId ;
}
