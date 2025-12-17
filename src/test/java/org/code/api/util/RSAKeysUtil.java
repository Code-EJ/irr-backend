package org.code.api.util;

import java.io.File;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

/**
 * É uma classe utilitaria pra carregar as chaves RSA usadas na aplicação durante a fase de teste.
 * Pelo contexto do Spring Context não estar carregado (então não existe dependency injection, singleton...), a gente chama essas funções
 * pra carregar as chaves diretamente dos arquivos na pasta resources (public.key e private.key).
 */
public class RSAKeysUtil {
  private static String readKeyFile(String path) {
    ClassLoader classLoader = RSAKeysUtil.class.getClassLoader();
    File resourceFile = new File(classLoader.getResource(path).getFile());

    try (Scanner scanner = new Scanner(resourceFile)) {
      scanner.useDelimiter("\\A");
      String content = scanner.hasNext() ? scanner.next() : "";
      return content;
    } catch (java.io.FileNotFoundException e) {
      throw new RuntimeException("Could not read key file: " + path, e);
    }    
  }

  private static RSAPrivateKey convertStringToPrivateKey(String keyContent) {
    try {
      String cleanedKey = keyContent
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s", "");
      
      byte[] decodedKey = Base64.getDecoder().decode(cleanedKey);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
      
      return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    } catch (Exception e) {
      throw new RuntimeException("Could not convert key content to RSAPrivateKey", e);
    }

  }

  private static RSAPublicKey convertStringToPublicKey(String keyContent) {
    try {
      String cleanedKey = keyContent
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replaceAll("\\s", "");

      byte[] decodedKey = Base64.getDecoder().decode(cleanedKey);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);

      return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    } catch (Exception e) {
      throw new RuntimeException("Could not convert key content to RSAPublicKey", e);
    }
  }

  public static RSAPrivateKey getPrivateKey() {
    String privateKeyContent = readKeyFile("private.key");

    return RSAKeysUtil.convertStringToPrivateKey(privateKeyContent);
  }

  public static RSAPublicKey getPublicKey() {
    String publicKeyContent = readKeyFile("public.key");

    return RSAKeysUtil.convertStringToPublicKey(publicKeyContent);
  }
}
