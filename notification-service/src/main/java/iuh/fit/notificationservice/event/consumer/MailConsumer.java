package iuh.fit.notificationservice.event.consumer;

import iuh.fit.notificationservice.event.payload.PasswordResetEvent;
import iuh.fit.notificationservice.event.payload.UserRegisteredEvent;
import iuh.fit.notificationservice.service.SesMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailConsumer {

    private final SesMailService sesMailService;

    @Value("${mychatapp.mail.from:}")
    private String mailFrom;

    @Value("${mychatapp.mail.registration-enabled:false}")
    private boolean registrationMailEnabled;

    @RabbitListener(queues = "${rabbitmq.mail.queue}")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received event: {}", event);

        if (!registrationMailEnabled) {
            log.info("Skip OTP email for {} (registration mail disabled)", event.getEmail());
            return;
        }

        try {
            String htmlContent = buildOtpEmailHtml(event.getUsername(), event.getOtp(), event.getOtpExpiryMinutes());
            sesMailService.sendHtml(
                    event.getEmail(),
                    "Xác nhận đăng ký tài khoản – Mã OTP của bạn",
                    htmlContent
            );
            log.info("OTP email sent from {} to: {}", mailFrom, event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", event.getEmail(), e.getMessage(), e);
            throw new IllegalStateException("Gửi mail OTP SES thất bại", e);
        }
    }


    @RabbitListener(queues = "auth.password-reset.queue")
    public void handlePasswordReset(PasswordResetEvent event) {
        log.info("[MailConsumer] Received password reset event for email: {}", event.getEmail());

        try {
            String htmlContent = buildPasswordResetEmailHtml(event.getResetLink(), event.getExpiryMinutes());
            sesMailService.sendHtml(
                    event.getEmail(),
                    "Đặt lại mật khẩu – MyChatApp",
                    htmlContent
            );
            log.info("[MailConsumer] Password reset email sent to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("[MailConsumer] Failed to send password reset email to {}: {}", event.getEmail(), e.getMessage(), e);
            throw new IllegalStateException("Gửi mail reset password SES thất bại", e);
        }
    }

    @RabbitListener(queues = "auth.password-reset-confirmed.queue")
    public void handlePasswordResetConfirmation(PasswordResetEvent event) {
        log.info("[MailConsumer] Received password reset confirmation event for email: {}", event.getEmail());

        try {
            String htmlContent = buildPasswordResetConfirmationEmailHtml(event.getEmail());
            sesMailService.sendHtml(
                    event.getEmail(),
                    "Mật khẩu của bạn vừa được thay đổi – MyChatApp",
                    htmlContent
            );
            log.info("[MailConsumer] Password reset confirmation email sent to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("[MailConsumer] Failed to send confirmation email to {}: {}", event.getEmail(), e.getMessage(), e);
            // Không throw exception vì mật khẩu đã được reset thành công
            log.warn("[MailConsumer] Skipping confirmation email error for {}", event.getEmail());
        }
    }



    private String buildOtpEmailHtml(String username, String otp, int expiryMinutes) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:12px;overflow:hidden;
                                  box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                      <!-- Header -->
                      <tr>
                        <td style="background:linear-gradient(135deg,#4f46e5,#7c3aed);
                                   padding:36px 40px;text-align:center;">
                          <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:700;
                                     letter-spacing:0.5px;">Xác nhận tài khoản</h1>
                          <p style="margin:8px 0 0;color:#c7d2fe;font-size:14px;">MyChatApp</p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:40px 40px 32px;">
                          <p style="margin:0 0 16px;color:#374151;font-size:15px;">
                            Xin chào <strong>%s</strong>,
                          </p>
                          <p style="margin:0 0 28px;color:#6b7280;font-size:14px;line-height:1.6;">
                            Cảm ơn bạn đã đăng ký tài khoản. Vui lòng dùng mã OTP bên dưới
                            để hoàn tất xác nhận:
                          </p>

                          <!-- OTP Box -->
                          <div style="background:#f5f3ff;border:2px dashed #7c3aed;
                                      border-radius:10px;padding:24px;text-align:center;
                                      margin-bottom:28px;">
                            <p style="margin:0 0 6px;color:#7c3aed;font-size:12px;
                                      font-weight:600;letter-spacing:1px;text-transform:uppercase;">
                              Mã xác nhận OTP
                            </p>
                            <p style="margin:0;color:#4f46e5;font-size:40px;font-weight:800;
                                      letter-spacing:12px;">
                              %s
                            </p>
                          </div>

                          <!-- Expiry notice -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="background:#fef3c7;border-radius:8px;margin-bottom:28px;">
                            <tr>
                              <td style="padding:12px 16px;">
                                <p style="margin:0;color:#92400e;font-size:13px;">
                                  ⏱ Mã OTP có hiệu lực trong <strong>%d phút</strong>.
                                  Vui lòng không chia sẻ mã này với bất kỳ ai.
                                </p>
                              </td>
                            </tr>
                          </table>

                          <p style="margin:0;color:#9ca3af;font-size:12px;line-height:1.6;">
                            Nếu bạn không thực hiện đăng ký này, hãy bỏ qua email hoặc
                            liên hệ hỗ trợ ngay lập tức.
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#f9fafb;padding:20px 40px;text-align:center;
                                   border-top:1px solid #e5e7eb;">
                          <p style="margin:0;color:#9ca3af;font-size:12px;">
                            © 2025 MyChatApp · Email này được gửi tự động, vui lòng không trả lời.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(username, otp, expiryMinutes > 0 ? expiryMinutes : 5);
    }

    private String buildPasswordResetEmailHtml(String resetLink, int expiryMinutes) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:12px;overflow:hidden;
                                  box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                      <!-- Header -->
                      <tr>
                        <td style="background:linear-gradient(135deg,#f97316,#fb923c);
                                   padding:36px 40px;text-align:center;">
                          <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:700;
                                     letter-spacing:0.5px;">Đặt lại mật khẩu</h1>
                          <p style="margin:8px 0 0;color:#fed7aa;font-size:14px;">MyChatApp</p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:40px 40px 32px;">
                          <p style="margin:0 0 16px;color:#374151;font-size:15px;">
                            Xin chào,
                          </p>
                          <p style="margin:0 0 28px;color:#6b7280;font-size:14px;line-height:1.6;">
                            Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.
                            Click vào nút bên dưới để tạo mật khẩu mới:
                          </p>

                          <!-- Reset Button -->
                          <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:28px;">
                            <tr>
                              <td align="center">
                                <a href="%s"
                                   style="display:inline-block;background:linear-gradient(135deg,#f97316,#fb923c);
                                          color:#ffffff;text-decoration:none;padding:14px 40px;
                                          border-radius:8px;font-weight:600;font-size:15px;
                                          transition:transform 0.2s;">
                                  Đặt lại mật khẩu
                                </a>
                              </td>
                            </tr>
                          </table>

                          <!-- Expiry notice -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="background:#fef3c7;border-radius:8px;margin-bottom:28px;">
                            <tr>
                              <td style="padding:12px 16px;">
                                <p style="margin:0;color:#92400e;font-size:13px;">
                                  ⏱ Link này có hiệu lực trong <strong>%d phút</strong>.
                                  Vì lý do bảo mật, nếu bạn không sử dụng link này, nó sẽ tự động hết hạn.
                                </p>
                              </td>
                            </tr>
                          </table>

                          <!-- Alternative link -->
                          <p style="margin:0 0 16px;color:#9ca3af;font-size:12px;line-height:1.6;">
                            Nếu nút không hoạt động, hãy copy link này vào trình duyệt:
                          </p>
                          <p style="margin:0 0 16px;color:#6366f1;font-size:12px;word-break:break-all;">
                            %s
                          </p>

                          <!-- Security notice -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="background:#eff6ff;border-left:4px solid #3b82f6;margin-top:20px;">
                            <tr>
                              <td style="padding:12px 16px;">
                                <p style="margin:0;color:#1e40af;font-size:12px;">
                                  🔒 <strong>Lưu ý bảo mật:</strong> Nếu bạn không yêu cầu đặt lại mật khẩu,
                                  vui lòng bỏ qua email này. Tài khoản của bạn sẽ an toàn.
                                </p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#f9fafb;padding:20px 40px;text-align:center;
                                   border-top:1px solid #e5e7eb;">
                          <p style="margin:0;color:#9ca3af;font-size:12px;">
                            © 2025 MyChatApp · Email này được gửi tự động, vui lòng không trả lời.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(resetLink, expiryMinutes, resetLink);
    }

    private String buildPasswordResetConfirmationEmailHtml(String email) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            </head>
            <body style="margin:0;padding:0;background-color:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f6f9;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="480" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:12px;overflow:hidden;
                                  box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                      <!-- Header -->
                      <tr>
                        <td style="background:linear-gradient(135deg,#10b981,#34d399);
                                   padding:36px 40px;text-align:center;">
                          <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:700;
                                     letter-spacing:0.5px;">✓ Mật khẩu đã được thay đổi</h1>
                          <p style="margin:8px 0 0;color:#a7f3d0;font-size:14px;">MyChatApp</p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:40px 40px 32px;">
                          <p style="margin:0 0 16px;color:#374151;font-size:15px;">
                            Xin chào,
                          </p>
                          <p style="margin:0 0 28px;color:#6b7280;font-size:14px;line-height:1.6;">
                            Mật khẩu của bạn đã được thay đổi thành công. Tài khoản của bạn hiện được bảo vệ
                            bằng mật khẩu mới.
                          </p>

                          <!-- Success check -->
                          <div style="background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;
                                      padding:16px;text-align:center;margin-bottom:28px;">
                            <p style="margin:0;color:#15803d;font-size:32px;">✓</p>
                            <p style="margin:8px 0 0;color:#166534;font-size:14px;font-weight:600;">
                              Mật khẩu đã cập nhật
                            </p>
                          </div>

                          <!-- Info box -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="background:#f3f4f6;border-radius:8px;">
                            <tr>
                              <td style="padding:16px;">
                                <p style="margin:0 0 8px;color:#374151;font-size:12px;font-weight:600;">
                                  Email tài khoản:
                                </p>
                                <p style="margin:0;color:#6b7280;font-size:13px;">
                                  %s
                                </p>
                              </td>
                            </tr>
                          </table>

                          <!-- Security note -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="background:#fef3c7;border-radius:8px;margin-top:20px;margin-bottom:20px;">
                            <tr>
                              <td style="padding:12px 16px;">
                                <p style="margin:0;color:#92400e;font-size:12px;">
                                  💡 <strong>Lưu ý:</strong> Nếu bạn không thực hiện thay đổi này, hãy liên hệ
                                  với chúng tôi ngay lập tức. Bảo mật tài khoản của bạn là ưu tiên hàng đầu.
                                </p>
                              </td>
                            </tr>
                          </table>

                          <p style="margin:0;color:#9ca3af;font-size:12px;line-height:1.6;">
                            Nếu bạn có bất kỳ câu hỏi hoặc cần hỗ trợ, vui lòng liên hệ với đội hỗ trợ của chúng tôi.
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#f9fafb;padding:20px 40px;text-align:center;
                                   border-top:1px solid #e5e7eb;">
                          <p style="margin:0;color:#9ca3af;font-size:12px;">
                            © 2025 MyChatApp · Email này được gửi tự động, vui lòng không trả lời.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(email);
    }
  }