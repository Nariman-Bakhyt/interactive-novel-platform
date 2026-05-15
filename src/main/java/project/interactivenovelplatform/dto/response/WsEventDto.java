package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WsEventDto<T> {
    private WsDomain domain;    // CHAT или SOCIAL
    private String type;        // Конкретный Enum в виде строки (NEW_MESSAGE, FRIEND_REQUEST_SENT)
    private T payload;          // Данные
}

