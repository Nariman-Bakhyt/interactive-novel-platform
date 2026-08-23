package project.interactivenovelplatform.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateGroupRequest {
    private String title ;
    private MultipartFile avatarUrl;
    private List<Long> memberIds ;
}
