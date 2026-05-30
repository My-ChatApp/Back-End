package iuh.fit.userservice.service;

import iuh.fit.userservice.config.MediaProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class S3ObjectStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private MediaProperties mediaProperties;

    @InjectMocks
    private S3ObjectStorageService s3ObjectStorageService;

    @Test
    void uploadAvatar_emptyFile_throwsIllegalArgument() {
        ReflectionTestUtils.setField(s3ObjectStorageService, "bucket", "test-bucket");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> s3ObjectStorageService.uploadAvatar("user-1", file));
    }

    @Test
    void uploadAvatar_nonImage_throwsIllegalArgument() {
        ReflectionTestUtils.setField(s3ObjectStorageService, "bucket", "test-bucket");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("text/plain");

        assertThrows(IllegalArgumentException.class,
                () -> s3ObjectStorageService.uploadAvatar("user-1", file));
    }

    @Test
    void keyFromPublicUrl_managedUrl_returnsKey() {
        when(mediaProperties.normalizedPublicBaseUrl()).thenReturn("https://cdn.example.com");
        String key = s3ObjectStorageService.keyFromPublicUrl(
                "https://cdn.example.com/users/u1/avatar.jpg");

        assertEquals("users/u1/avatar.jpg", key);
    }

    @Test
    void keyFromPublicUrl_unmanagedUrl_returnsNull() {
        when(mediaProperties.normalizedPublicBaseUrl()).thenReturn("https://cdn.example.com");
        assertNull(s3ObjectStorageService.keyFromPublicUrl("https://other.cdn/img.jpg"));
    }

    @Test
    void isManagedPublicUrl_recognizesBaseUrl() {
        when(mediaProperties.normalizedPublicBaseUrl()).thenReturn("https://cdn.example.com");
        assertTrue(s3ObjectStorageService.isManagedPublicUrl("https://cdn.example.com/a/b.jpg"));
        assertFalse(s3ObjectStorageService.isManagedPublicUrl("https://other.example.com/a.jpg"));
    }

    @Test
    void deleteIfExists_defaultAvatarKey_skipsS3() {
        ReflectionTestUtils.setField(s3ObjectStorageService, "bucket", "test-bucket");
        s3ObjectStorageService.deleteIfExists(MediaProperties.DEFAULT_AVATAR_KEY);

        verify(s3Client, never()).deleteObject(org.mockito.ArgumentMatchers.any(
                software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));
    }
}
