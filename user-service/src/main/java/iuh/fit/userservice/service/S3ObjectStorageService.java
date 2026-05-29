package iuh.fit.userservice.service;

import iuh.fit.userservice.config.MediaProperties;
import iuh.fit.userservice.exception.ImageUploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ObjectStorageService {

    private final S3Client s3Client;
    private final MediaProperties mediaProperties;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public record UploadResult(String publicUrl, String key) {
    }

    public UploadResult uploadAvatar(String userId, MultipartFile file) {
        validateImage(file);
        if (bucket == null || bucket.isBlank()) {
            throw new ImageUploadException("S3 bucket is not configured", null);
        }
        if (mediaProperties.normalizedPublicBaseUrl().isBlank()) {
            throw new ImageUploadException("MEDIA_PUBLIC_BASE_URL is not configured", null);
        }

        String ext = extensionFromFileName(file.getOriginalFilename());
        String key = "users/" + userId + "/avatar/" + UUID.randomUUID() + ext;
        String contentType = normalizeContentType(file.getContentType());

        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(put, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new ImageUploadException("Failed to read upload file", e);
        } catch (Exception e) {
            throw new ImageUploadException("Failed to upload to S3", e);
        }

        return new UploadResult(mediaProperties.publicUrlForKey(key), key);
    }

    public void deleteIfExists(String key) {
        if (key == null || key.isBlank() || bucket == null || bucket.isBlank()) {
            return;
        }
        if (MediaProperties.DEFAULT_AVATAR_KEY.equals(key)) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    public String keyFromPublicUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        String base = mediaProperties.normalizedPublicBaseUrl();
        if (base.isBlank() || !publicUrl.startsWith(base + "/")) {
            return null;
        }
        return publicUrl.substring(base.length() + 1);
    }

    public boolean isManagedPublicUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String base = mediaProperties.normalizedPublicBaseUrl();
        return !base.isBlank() && url.startsWith(base + "/");
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be null or empty");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType.split(";")[0].trim().toLowerCase();
    }

    private static String extensionFromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        String ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        return ext.length() <= 10 ? ext : ".jpg";
    }
}
