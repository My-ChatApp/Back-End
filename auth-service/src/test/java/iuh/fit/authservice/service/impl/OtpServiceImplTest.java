package iuh.fit.authservice.service.impl;

import iuh.fit.authservice.entity.OtpType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpServiceImpl(redisTemplate);
        ReflectionTestUtils.setField(otpService, "expiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);
        ReflectionTestUtils.setField(otpService, "resendCooldownSeconds", 60);
    }

    private void stubValueOps() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void sendOtp_storesWithTypePrefix() {
        stubValueOps();
        when(redisTemplate.hasKey("otp:cooldown:REGISTER:user@b.com")).thenReturn(false);

        otpService.sendOtp("User@B.com", OtpType.REGISTER);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), any(), eq(5L), eq(TimeUnit.MINUTES));
        assertTrue(keyCaptor.getValue().startsWith("otp:REGISTER:"));
    }

    @Test
    void verifyOtp_wrongCode_returnsFalse() {
        stubValueOps();
        when(valueOperations.get("otp:LOGIN:user@b.com")).thenReturn("123456");

        boolean result = otpService.verifyOtp("user@b.com", "000000", OtpType.LOGIN);

        assertFalse(result);
        verify(valueOperations).increment("otp:attempts:LOGIN:user@b.com");
    }

    @Test
    void verifyOtp_correctCode_deletesKey() {
        stubValueOps();
        when(valueOperations.get("otp:LOGIN:user@b.com")).thenReturn("123456");

        boolean result = otpService.verifyOtp("user@b.com", "123456", OtpType.LOGIN);

        assertTrue(result);
        verify(redisTemplate).delete("otp:LOGIN:user@b.com");
    }

    @Test
    void assertResendAllowed_throwsWhenCooldownActive() {
        when(redisTemplate.hasKey("otp:cooldown:REGISTER:user@b.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> otpService.sendOtp("user@b.com", OtpType.REGISTER));

        verify(valueOperations, never()).set(any(), any(), anyLong(), any());
    }
}
