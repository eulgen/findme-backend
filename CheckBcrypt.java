import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class CheckBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("Matches 'password': " + encoder.matches("password", "$2a$10$8.UnVuG9HLB0fT7veL.6V.C.h7h1RzCj/gG4gCqZlW1w1A5xO03dO"));
        System.out.println("Matches 'Password123!': " + encoder.matches("Password123!", "$2a$10$8.UnVuG9HLB0fT7veL.6V.C.h7h1RzCj/gG4gCqZlW1w1A5xO03dO"));
    }
}
