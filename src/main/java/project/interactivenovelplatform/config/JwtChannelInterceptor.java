package project.interactivenovelplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import project.interactivenovelplatform.security.JwtTokenProvider;
import project.interactivenovelplatform.repository.ConversationMemberRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.codec.binary.Hex;
import project.interactivenovelplatform.security.UserPrincipal;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {
    private final JwtTokenProvider jwtService; 
    private final UserDetailsService userDetailsService;
    @Lazy
    private final ConversationMemberRepository conversationMemberRepository;

    @Value("${app.guest.secret}")
    private String hashSecret;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // WebSocket-соединения работают поверх постоянного TCP и не проходят через HTTP Filter Chain. 
        // Перехватываем команду CONNECT для ручного извлечения токена из заголовков STOMP и ассоциации принципала с сессией.
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtService.getUsernameFromToken(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtService.validateToken(token)) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                        
                        accessor.setUser(authentication);
                    }
                }
            } else {
                // Попытка извлечь гостевой токен из STOMP заголовков
                String guestToken = accessor.getFirstNativeHeader("Guest-Id");
                if (guestToken != null && validateGuestToken(guestToken)) {
                    String guestId = guestToken.split("\\.")[0];
                    AnonymousAuthenticationToken guestAuth = new AnonymousAuthenticationToken(
                            "guest-key", "guest_" + guestId, AuthorityUtils.createAuthorityList("ROLE_GUEST"));
                    accessor.setUser(guestAuth);
                }
            }
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/chat.")) {
                try {
                    Long conversationId = Long.parseLong(destination.substring("/topic/chat.".length()));
                    java.security.Principal principal = accessor.getUser();
                    
                    if (principal == null) {
                        throw new AccessDeniedException("User must be authenticated to subscribe to chat");
                    }
                    if (principal instanceof UsernamePasswordAuthenticationToken auth) {
                        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
                        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userPrincipal.getId())) {
                            throw new AccessDeniedException("User is not a member of this conversation");
                        }
                    } else if (principal.getName().startsWith("guest_")) {
                        throw new AccessDeniedException("Guests cannot subscribe to chat topics");
                    } else {
                        throw new AccessDeniedException("Invalid authentication type");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid conversation ID format in destination");
                }
            } else if (destination != null && destination.startsWith("/topic/user.")) {
                try {
                    Long userId = Long.parseLong(destination.substring("/topic/user.".length()));
                    java.security.Principal principal = accessor.getUser();
                    
                    if (principal == null) {
                        throw new AccessDeniedException("User must be authenticated");
                    }
                    if (principal instanceof UsernamePasswordAuthenticationToken auth) {
                        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
                        if (!userPrincipal.getId().equals(userId)) {
                            throw new AccessDeniedException("Cannot subscribe to other user's topic");
                        }
                    } else if (principal.getName().startsWith("guest_")) {
                        throw new AccessDeniedException("Guests cannot subscribe to user topics");
                    } else {
                        throw new AccessDeniedException("Invalid authentication type");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid user ID format in destination");
                }
            }
        }
        return message;
    }

    private boolean validateGuestToken(String token) {
        if (token == null || !token.contains(".")) return false;
        String[] parts = token.split("\\.");
        if (parts.length != 2) return false;
        return parts[1].equals(generateSignature(parts[0]));
    }

    private String generateSignature(String data) {
        try {
            String algorithm = "HmacSHA256";
            SecretKeySpec secretKeySpec = new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
}