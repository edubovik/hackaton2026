package com.chatapp.auth;

import com.chatapp.auth.entity.PasswordResetToken;
import com.chatapp.auth.entity.User;
import com.chatapp.auth.repository.PasswordResetTokenRepository;
import com.chatapp.auth.repository.UserRepository;
import com.chatapp.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserRepository userRepo;
    @Mock PasswordResetTokenRepository resetTokenRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;

    @InjectMocks PasswordResetService service;

    @Test
    void requestReset_unknownEmail_doesNothing() {
        when(userRepo.findByEmail("nobody@x.com")).thenReturn(Optional.empty());

        service.requestReset("nobody@x.com");

        verifyNoInteractions(resetTokenRepo, emailService);
    }

    @Test
    void requestReset_knownEmail_savesTokenAndSendsEmail() {
        User user = new User();
        user.setEmail("alice@x.com");
        when(userRepo.findByEmail("alice@x.com")).thenReturn(Optional.of(user));
        when(resetTokenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.requestReset("alice@x.com");

        ArgumentCaptor<PasswordResetToken> cap = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepo).save(cap.capture());
        PasswordResetToken saved = cap.getValue();
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(OffsetDateTime.now());

        verify(emailService).sendPasswordResetEmail(eq("alice@x.com"), eq(saved.getToken()));
    }

    @Test
    void resetPassword_invalidToken_throws() {
        when(resetTokenRepo.findByToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("bad", "newpw"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void resetPassword_usedToken_throws() {
        PasswordResetToken prt = tokenWith(false);
        prt.setUsed(true);
        when(resetTokenRepo.findByToken("tok")).thenReturn(Optional.of(prt));

        assertThatThrownBy(() -> service.resetPassword("tok", "newpw"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void resetPassword_expiredToken_throws() {
        PasswordResetToken prt = tokenWith(false);
        prt.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        when(resetTokenRepo.findByToken("tok")).thenReturn(Optional.of(prt));

        assertThatThrownBy(() -> service.resetPassword("tok", "newpw"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndMarksUsed() {
        User user = new User();
        user.setPassword("old-hash");
        PasswordResetToken prt = tokenWith(false);
        prt.setUser(user);
        when(resetTokenRepo.findByToken("tok")).thenReturn(Optional.of(prt));
        when(passwordEncoder.encode("newpw")).thenReturn("new-hash");
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(resetTokenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.resetPassword("tok", "newpw");

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(prt.isUsed()).isTrue();
    }

    private PasswordResetToken tokenWith(boolean used) {
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken("tok");
        prt.setExpiresAt(OffsetDateTime.now().plusHours(1));
        prt.setUsed(used);
        User user = new User();
        prt.setUser(user);
        return prt;
    }
}
