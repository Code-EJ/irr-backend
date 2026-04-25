package org.code.api.domain.ports;

import org.code.api.domain.enums.UserType;
import java.util.List;
import java.util.UUID;

/**
 * Provedor de Contexto do Ator.
 * Esta porta permite que as classes de serviço obtenham informações do usuário logado
 * sem depender diretamente de frameworks de segurança ou threads estáticas.
 */
public interface AuthenticatedUserProvider {

    /**
     * Retorna o UUID do usuário presente na sessão atual.
     */
    UUID getCurrentUserId();

    /**
     * Retorna a lista de permissões/perfis (Roles) do usuário logado.
     */
    List<UserType> getCurrentUserTypes();
}