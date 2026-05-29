package iuh.fit.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "media")
public class MediaProperties {

    public static final String DEFAULT_AVATAR_KEY = "static/default-avatar.jpg";

    private String publicBaseUrl = "";

    public String normalizedPublicBaseUrl() {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    public String publicUrlForKey(String key) {
        return normalizedPublicBaseUrl() + "/" + key;
    }

    public String defaultAvatarUrl() {
        return publicUrlForKey(DEFAULT_AVATAR_KEY);
    }
}
