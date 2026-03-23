package project.interactivenovelplatform.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Главная магия здесь
public class Metadata {
    private String type;
    private List<String> images;
    private String quoteText;
    private String anchorUrl;
}
