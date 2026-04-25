package org.code.api.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.enums.UserType;
import org.code.api.domain.exception.AuthError;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.models.user.User;
import org.code.api.domain.ports.AuthPort;
import org.code.api.domain.ports.HashingPort;
import org.code.api.domain.ports.TokenPort;
import org.code.api.infrastructure.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class AuthService implements AuthPort {

    private TokenPort tokenPort;
    private UserRepository userRepository;
    private HashingPort hashingPort;

    @Override
    @Transactional(readOnly = true)
    public String authenticate(String email, String senha) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthError.WrongCredentials(email, false));

        if (!hashingPort.compare(user.getSenha(), senha)) {
            throw new AuthError.WrongCredentials(email, true);
        }

        return tokenPort.createToken(
            Session.builder()
                .id(user.getId())
                .email(user.getEmail())
                .tipo(user.getTipo())
                .build()
        );
    }

    @Override
    public String register(
        String nome,
        String email,
        String senha,
        String createdBy
    ) {
        try {
            if (userRepository.existsByEmail(email)) {
                throw new AuthError.EmailOccupied(email);
            }

            Optional<User> creatorUser = createdBy != null
                ? userRepository.findById(UUID.fromString(createdBy))
                : Optional.empty();

            if (createdBy != null && creatorUser.isEmpty()) {
                throw new AuthError.CreatorUserInvalid(createdBy);
            }

            String encryptedPassword = hashingPort.encrypt(senha);

            LocalDateTime now = LocalDateTime.ofInstant(
                Instant.now(),
                ZoneId.systemDefault()
            );

            User user = userRepository.save(
                User.builder()
                    .nome(nome)
                    .senha(encryptedPassword)
                    .email(email)
                    .createdBy(creatorUser.orElse(null))
                    .tipo(UserType.REPRESENTANTE)
                    .updatedAt(now)
                    .createdAt(now)
                    .build()
            );

            return tokenPort.createToken(
                Session.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .tipo(UserType.REPRESENTANTE)
                    .build()
            );
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new AuthError.CreatorUserInvalid(
                createdBy,
                true,
                illegalArgumentException
            );
        }
    }

    @Override
    public String register(String nome, String email, String senha) {
        return register(nome, email, senha, null);
    }

    @Override
    public String renew(String token) {
        throw new UnsupportedOperationException("Unimplemented method 'renew'");
    }

    /**
     * Validação de sessão.
     *
     * Decodifica o token JWT e valida que o usuário existe no banco de dados.
     * - Se o token for inválido (algoritmo, formato, assinatura, expiração), lança {@link AuthError.InvalidToken}.
     * - Se o payload decodificado não corresponder a um usuário existente (id não encontrado), lança {@link AuthError.InvalidToken}.
     * - Se válido, retorna os dados da sessão (id, email, tipo, issuedAt, expiresAt).
     *
     * @param token token JWT a ser validado
     * @return objeto {@link Session} com os dados validados da sessão
     * @throws AuthError.InvalidToken quando o token for inválido ou o usuário não existir
     */
    @Override
    public Session getSessionDetails(String token) {
        Session session = tokenPort.decodeToken(token);
        Optional<User> userOptional = userRepository.findById(session.getId());

        log.debug(
            "Decoded session token:\nUser email: {}\nExpiresAt: {}, IssuedAt: {}",
            userOptional.get().getEmail(),
            session.getExpiresAt(),
            session.getIssuedAt()
        );

        return userOptional
            .map(user ->
                Session.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .tipo(user.getTipo())
                    .issuedAt(session.getIssuedAt())
                    .expiresAt(session.getExpiresAt())
                    .build()
            )
            .orElseThrow(() -> new AuthError.InvalidToken(token));
    }
}
