package org.code.api.infrastructure.security;

import org.code.api.domain.enums.UserType;
import org.code.api.domain.exception.AuthError;
import org.code.api.domain.ports.AuthenticatedUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador de Infraestrutura que implementa a porta {@link AuthenticatedUserProvider}.
 *
 * <p>Responsável por acoplar o framework de segurança (Spring Security) às necessidades
 * agnósticas da camada de Domínio/Serviço. O objetivo desta classe é garantir que os
 * serviços de negócio possam recuperar o identificador do usuário logado sem importar
 * bibliotecas do Spring ou conhecer os detalhes de implementação do JWT.
 *
 * @implNote Realiza validações de fallback robustas para lidar com cenários onde o
 * {@code SecurityContextHolder} contém tipos inesperados de Principal (ex: UUID vs String),
 * bem como barra preventivamente requisições do ator padrão do Spring ({@code "anonymousUser"}).
 *
 * @throws org.code.api.domain.exception.AuthError.Unauthorized se o contexto não possuir usuário válido.
 * @throws org.code.api.domain.exception.AuthError.InternalServerError se houver falha de conversão do identificador (ClassCastException).
 */
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
