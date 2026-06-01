package iuh.fit.notificationservice.event.consumer;

import iuh.fit.notificationservice.event.payload.UserRegisteredEvent;
import iuh.fit.notificationservice.service.SesMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailConsumerTest {

    @Mock
    private SesMailService sesMailService;

    @InjectMocks
    private MailConsumer mailConsumer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailConsumer, "mailFrom", "noreply@example.com");
        ReflectionTestUtils.setField(mailConsumer, "registrationMailEnabled", false);
    }

    @Test
    void handleUserRegistered_disabled_skipsSes() {
        UserRegisteredEvent event =
                new UserRegisteredEvent(
                        "test@gmail.com",
                        "devil",
                        "123456",
                        5
                );

        mailConsumer.handleUserRegistered(event);

        verify(sesMailService, never()).sendHtml(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handleUserRegistered_enabled_sendsMail() {
        ReflectionTestUtils.setField(mailConsumer, "registrationMailEnabled", true);
        UserRegisteredEvent event =
                new UserRegisteredEvent(
                        "test@gmail.com",
                        "devil",
                        "123456",
                        5
                );

        mailConsumer.handleUserRegistered(event);

        verify(sesMailService).sendHtml(
                eq("test@gmail.com"),
                eq("Xác nhận đăng ký tài khoản – Mã OTP của bạn"),
                argThat(html ->
                        html.contains("devil")
                                && html.contains("123456")
                                && html.contains("5 phút")));
    }
}
