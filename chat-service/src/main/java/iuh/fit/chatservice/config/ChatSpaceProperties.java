package iuh.fit.chatservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chat.space")
public class ChatSpaceProperties {

    private boolean enabled = true;
    private int timelineMaxSize = 300;
    /** Số tin tối đa mỗi lần GET /messages (mặc định). */
    private int defaultPageSize = 20;
    private int hydrateBatchSize = 20;
    private boolean dualWriteDynamodb = false;
}
