package project.interactivenovelplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.interactivenovelplatform.entity.MessageEntity;

import java.util.List;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class FinalizedMessageData {
    private MessageEntity message;
    private List<Long> memberIds;
}
