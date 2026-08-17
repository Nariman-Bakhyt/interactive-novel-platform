package project.interactivenovelplatform.repository;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import project.interactivenovelplatform.entity.NotificationType;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class NotificationBatchRepositoryImpl implements NotificationBatchRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<Long> batchInsertNotifications(List<Long> recipientIds, Long senderId, NotificationType type, Map<String, Object> metadata, Long relatedEntityId) {
        if (recipientIds.isEmpty()) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("INSERT INTO notifications (recipient_id, sender_id, type, metadata, related_entity_id, is_read, created_at) VALUES ");
        for (int i = 0; i < recipientIds.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, ?, ?::jsonb, ?, false, ?)");
        }
        sql.append(" RETURNING id");

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize metadata", e);
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());

        Object[] args = new Object[recipientIds.size() * 6];
        int idx = 0;
        for (Long recipientId : recipientIds) {
            args[idx++] = recipientId;
            args[idx++] = senderId;
            args[idx++] = type.name();
            args[idx++] = metadataJson;
            args[idx++] = relatedEntityId;
            args[idx++] = now;
        }

        return jdbcTemplate.queryForList(sql.toString(), Long.class, args);
    }
}
