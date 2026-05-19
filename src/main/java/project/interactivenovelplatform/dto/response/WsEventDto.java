package project.interactivenovelplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WsEventDto<T> {
    private WsDomain domain;    
    private String type;        
    private T payload;          
}

