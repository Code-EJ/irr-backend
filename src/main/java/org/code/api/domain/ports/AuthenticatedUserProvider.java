package org.code.api.domain.ports;

import org.code.api.domain.enums.UserRole;

import java.util.List;
import java.util.UUID;


/// Contrato de domínio para recuperar as informações do ator que está executando a ação.
public interface AuthenticatedUserProvider {
    UUID getCurrentUserId();
    List<UserRole>  getCurrentUserRoles();
}
