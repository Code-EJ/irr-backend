package org.code.api.domain.ports;

import org.code.api.domain.models.user.Session;

/**
 * Interface que define o contrato para provedores de tokens de segurança.
 * Especifica os métodos necessários para geração, decodificação e renovação de tokens.
 */
public interface TokenPort {

  /**
   * Gera um novo token com base na sessão fornecida.
   *
   * @param session Objeto {@link Session} contendo as informações da sessão do usuário.
   * @return Uma string representando o token gerado.
   */
  String createToken(Session session);

  /**
   * Decodifica um token e retorna a sessão correspondente.
   *
   * @param token Token a ser decodificado.
   * @return Um objeto {@link Session} representando a sessão decodificada.
   */
  Session decodeToken(String token);

  /**
   * Renova um token existente e retorna um novo token.
   *
   * @param token Token a ser renovado.
   * @return Uma string representando o novo token gerado.
   */
  String renewToken(String token);

  /**
   * Renova um token com base na sessão fornecida e retorna um novo token.
   *
   * @param session Objeto {@link Session} contendo as informações da sessão do usuário.
   * @return Uma string representando o novo token gerado.
   */
  String renewToken(Session session);
}