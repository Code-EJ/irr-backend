package org.code.api.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Provides ephemeral, process-local RSA keys without reading application credentials.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class RSAKeysUtil {
    private static final KeyPair KEYS = generate();
    private RSAKeysUtil() {}
    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Cannot generate test keys", exception);
        }
    }
    /** @return the process-local private test key */
    public static RSAPrivateKey getPrivateKey() { return (RSAPrivateKey) KEYS.getPrivate(); }
    /** @return the matching public test key */
    public static RSAPublicKey getPublicKey() { return (RSAPublicKey) KEYS.getPublic(); }
}
