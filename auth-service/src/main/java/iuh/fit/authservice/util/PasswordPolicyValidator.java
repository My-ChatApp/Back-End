package iuh.fit.authservice.util;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 100;

    public void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Mật khẩu phải từ 8 đến 100 ký tự");
        }
        if (!isPasswordComplex(password)) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải chứa ít nhất: 1 chữ hoa, 1 chữ thường, 1 chữ số, 1 ký tự đặc biệt"
            );
        }
    }

    public boolean isPasswordComplex(String password) {
        return password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/\\\\|`~].*");
    }
}
