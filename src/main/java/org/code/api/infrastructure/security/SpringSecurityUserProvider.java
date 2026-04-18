package org.code.api.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.code.api.domain.enums.UserType;
import org.code.api.domain.exception.AuthError;
import org.code.api.domain.models.user.Session;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serial;
import java.util.List;
import java.util.UUID;

///
@Component
public class SpringSecurityUserProvider implements AuthenticatedUserProvider {

    @Override
    public UUID getCurrentUserId() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthError.Unauthorized("Nenhum Usuario Autenticado encontrado no Contexto.");
        }


        Object principal = authentication.getPrincipal();

        try{
            if (principal instanceof UUID) {
                return (UUID) principal;
            }
            else if (principal instanceof String) {
                return (UUID) UUID.fromString((String) principal);
            }

            throw new IllegalArgumentException("Tipo de principal não suportado: " + principal.getClass().getName());
        }catch (ClassCastException e){
            throw new AuthError.InternalServerError("Falha ao extrair a identidade do usuário do token de segurança.");
        }
    }

    @Override
    public List<UserType> getCurrentUserTypes() {
        return List.of();
    }
}
