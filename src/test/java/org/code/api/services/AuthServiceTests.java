package org.code.api.services;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.enums.UserRole;
import org.code.api.domain.exception.AuthError;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.domain.ports.TokenPort;
import org.code.api.infrastructure.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Characterizes authentication rejection, credential hashing and current role resolution.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTests {
    @Mock TokenPort tokens;
    @Mock UserRepository users;
    @Mock EncryptionPort encryption;
    @InjectMocks AuthService service;

    /** Verifies that unknown email never issues token. */
    @Test void unknownEmailNeverIssuesToken() {
        when(users.findByEmail("unknown@example.test")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.authenticate("unknown@example.test", "password"))
            .isInstanceOf(AuthError.WrongCredentials.class);
        verifyNoInteractions(tokens, encryption);
    }
    /** Verifies that wrong password never issues token. */
    @Test void wrongPasswordNeverIssuesToken() {
        User user = user();
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(encryption.compare("hash", "wrong")).thenReturn(false);
        assertThatThrownBy(() -> service.authenticate(user.getEmail(), "wrong"))
            .isInstanceOf(AuthError.WrongCredentials.class);
        verifyNoInteractions(tokens);
    }
    /** Verifies that authentication uses stored identity and role. */
    @Test void authenticationUsesStoredIdentityAndRole() {
        User user = user();
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(encryption.compare("hash", "password")).thenReturn(true);
        when(tokens.createToken(any())).thenReturn("signed-token");
        assertThat(service.authenticate(user.getEmail(), "password")).isEqualTo("signed-token");
        ArgumentCaptor<Session> captured = ArgumentCaptor.forClass(Session.class);
        verify(tokens).createToken(captured.capture());
        assertThat(captured.getValue().getId()).isEqualTo(user.getId());
        assertThat(captured.getValue().getUserRole()).isEqualTo(UserRole.ADMINISTRATOR);
    }
    /** Verifies that registration persists hash rather than password. */
    @Test void registrationPersistsHashRatherThanPassword() {
        when(encryption.encrypt("password")).thenReturn("hash");
        when(users.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0); user.setId(UUID.randomUUID()); return user;
        });
        service.register("Test User", "user@example.test", "password");
        ArgumentCaptor<User> captured = ArgumentCaptor.forClass(User.class);
        verify(users).save(captured.capture());
        assertThat(captured.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(captured.getValue().getUserRole()).isEqualTo(UserRole.REPRESENTATIVE);
    }
    /** Verifies that duplicate email never persists or issues token. */
    @Test void duplicateEmailNeverPersistsOrIssuesToken() {
        when(users.existsByEmail("user@example.test")).thenReturn(true);
        assertThatThrownBy(() -> service.register("User", "user@example.test", "password"))
            .isInstanceOf(AuthError.EmailOccupied.class);
        verify(users, never()).save(any());
        verifyNoInteractions(tokens, encryption);
    }
    /** Verifies that removed user cannot restore session from valid token. */
    @Test void removedUserCannotRestoreSessionFromValidToken() {
        Session session = Session.builder().id(UUID.randomUUID()).build();
        when(tokens.decodeToken("token")).thenReturn(session);
        when(users.findById(session.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSessionDetails("token"))
            .isInstanceOf(AuthError.InvalidToken.class);
    }
    /** Verifies that session uses current role rather than stale token role. */
    @Test void sessionUsesCurrentRoleRatherThanStaleTokenRole() {
        User user = user();
        when(tokens.decodeToken("token")).thenReturn(Session.builder().id(user.getId())
            .userRole(UserRole.REPRESENTATIVE).build());
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        assertThat(service.getSessionDetails("token").getUserRole()).isEqualTo(UserRole.ADMINISTRATOR);
    }
    private User user() {
        return User.builder().id(UUID.randomUUID()).email("admin@example.test")
            .passwordHash("hash").userRole(UserRole.ADMINISTRATOR).build();
    }
}
