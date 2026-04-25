package org.code.api.domain.ports;

import org.code.api.domain.models.user.Session;

/**
 * Porta de Entrada (‘Input’ Port) para operações de Autenticação e Gestão de Identidade.
 * Define o contrato para os casos de uso de ‘login’, registro e análise de sessão.
 */
public interface AuthPort {

  /**
   * Valida credenciais e gera uma credencial de acesso (token).
   * @param email E-mail do usuário.
   * @param senha Senha em texto plano.
   * @return Token JWT assinado.
   */
  String authenticate(String email, String senha);

  /**
   * Registra um novo usuário vinculado a um criador (gestor).
   * @param createdBy ID do usuário que está realizando o cadastro.
   */
  String register(String nome, String email, String senha, String createdBy);

  /**
   * Autoregistro de novo usuário (Representante).
   */
  String register(String nome, String email, String senha);

  /**
   * Estende a validade de um token ativo.
   */
  String renew(String token);

  /**
   * Decodifica um token e extrai os metadados da sessão ativa.
   * @throws org.code.api.domain.exception.AuthError se o token for inválido ou expirado.
   */
  Session getSessionDetails(String token);
}