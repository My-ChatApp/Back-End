package iuh.fit.authservice.entity;

public enum OtpType {
    REGISTER("Xác thực email đăng ký"),
    LOGIN("Xác thực email đăng nhập"),
    CHANGE_PASSWORD("Xác thực email đổi mật khẩu");

    private final String emailSubject;

    OtpType(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailSubject() {
        return emailSubject;
    }
}
