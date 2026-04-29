package project.interactivenovelplatform.service;

import org.springframework.transaction.annotation.Transactional;
import project.interactivenovelplatform.entity.VerificationTokenType;

public interface VerificationService {
    @Transactional
    void sendVerificationCode(Long userId, VerificationTokenType type, String pendingValue);

    @Transactional
    void verifyCode(Long userId, String code, VerificationTokenType type);
}
