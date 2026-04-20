package com.chatapp.auth;

import com.chatapp.auth.entity.PasswordResetToken;
import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.PasswordResetTokenRepository;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.common.exception.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {

    private static final int TOKEN_TTL_HOURS = 1;

    private final UserRepository userRepo;
    private final PasswordResetTokenRepository resetTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(UserRepository userRepo,
                                PasswordResetTokenRepository resetTokenRepo,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.userRepo = userRepo;
        this.resetTokenRepo = resetTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void requestReset(String email) {
        userRepo.findByEmail(email).ifPresent(user -> {
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUser(user);
            prt.setToken(UUID.randomUUID().toString());
            prt.setExpiresAt(OffsetDateTime.now().plusHours(TOKEN_TTL_HOURS));
            resetTokenRepo.save(prt);
            emailService.sendPasswordResetEmail(user.getEmail(), prt.getToken());
        });
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = resetTokenRepo.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        if (prt.isUsed()) {
            throw new BadRequestException("Token has already been used");
        }
        if (prt.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Token has expired");
        }

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        prt.setUsed(true);
        resetTokenRepo.save(prt);
    }
}
