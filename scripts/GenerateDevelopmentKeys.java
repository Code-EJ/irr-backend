import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Generates development RSA keys without implicitly rotating existing credentials.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class GenerateDevelopmentKeys {
  /**
   * Creates a development-only key pair in the current repository.
   *
   * @param args unused command-line arguments
   * @throws Exception if generation fails or keys already exist
   */
  public static void main(String[] args) throws Exception {
    Path directory = Path.of(".local", "keys");
    Files.createDirectories(directory);
    Path privateFile = directory.resolve("private.pem");
    Path publicFile = directory.resolve("public.pem");
    if (Files.exists(privateFile) || Files.exists(publicFile)) {
      throw new IllegalStateException("Keys already exist; refusing implicit rotation");
    }
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(3072);
    var keys = generator.generateKeyPair();
    Files.writeString(privateFile, pem("PRIVATE", keys.getPrivate().getEncoded()));
    Files.writeString(publicFile, pem("PUBLIC", keys.getPublic().getEncoded()));
    System.out.println("Development keys created in .local/keys; keep them out of Git.");
  }

  private static String pem(String kind, byte[] bytes) {
    return "-----BEGIN "
        + kind
        + " KEY-----\n"
        + Base64.getMimeEncoder(64, new byte[] {10}).encodeToString(bytes)
        + "\n-----END "
        + kind
        + " KEY-----\n";
  }
}
