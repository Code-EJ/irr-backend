package org.code.api.domain.ports;

/**
 * Porta de Saída (Output Port) para operações de Hashing de segurança.
 * * <p>Esta porta é especificamente projetada para transformar segredos (senhas)
 * em representações unidirecionais não reversíveis, garantindo que, mesmo em caso
 * de vazamento do banco de dados, o segredo original permaneça protegido.</p>
 * @implNote Não deve ser utilizada para dados que exijam recuperação posterior (decryption).
 */
public interface HashingPort {

  /**
   * Gera um hash seguro (unidirecional) a partir de uma string.
   * Tipicamente utiliza algoritmos como BCrypt ou Argon2 com salt automático.
   * @param plainText A string original (senha em texto plano).
   * @return O hash resultante formatado para armazenamento.
   */
  String encrypt(String plainText);

  /**
   * Verifica se uma string em texto plano corresponde a um hash previamente gerado.
   * @param plainText A string a ser verificada.
   * @param storedHash O hash armazenado no banco de dados.
   * @return true se a correspondência for válida.
   */
  boolean compare(String plainText, String storedHash);
}
