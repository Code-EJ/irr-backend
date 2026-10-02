import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Standalone legacy BCrypt fixture demonstration, excluded from the application build.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public class TestHash {
  /** Prints matches and hashes for the hardcoded non-secret demonstration fixture. */
  public static void main(String[] args) {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    boolean match =
        encoder.matches("123456", "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjQJU/B6cK");
    System.out.println("Match: " + match);
    System.out.println("New Hash: " + encoder.encode("123456"));
  }
}
