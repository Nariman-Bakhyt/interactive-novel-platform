package project.interactivenovelplatform.service;

import project.interactivenovelplatform.entity.VerificationTokenType;

public interface EmailService {
    void sendCode(String to, String code, VerificationTokenType type);
}
