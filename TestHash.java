import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class TestHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean match = encoder.matches("123456", "$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjQJU/B6cK");
        System.out.println("Match: " + match);
        System.out.println("New Hash: " + encoder.encode("123456"));
    }
}
