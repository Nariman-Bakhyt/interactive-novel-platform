package project.interactivenovelplatform.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import project.interactivenovelplatform.entity.VerificationTokenType;
import project.interactivenovelplatform.service.EmailService;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;

    @Async("taskExecutor")
    public void sendCode(String to, String code, VerificationTokenType type) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Код подтверждения");
        message.setText("Ваш код для " + type + ": " + code + "\nДействителен 30 минут.");
        mailSender.send(message);
    }
}
