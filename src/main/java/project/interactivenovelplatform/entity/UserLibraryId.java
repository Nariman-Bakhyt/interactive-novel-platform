package project.interactivenovelplatform.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.HashCodeExclude;

import java.io.Serializable;

@Embeddable
@Getter@Setter
@EqualsAndHashCode
public class UserLibraryId implements Serializable {
    private Long userId;
    private Long novelId;
}
