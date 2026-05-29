package iuh.fit.notificationservice.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateNotificationRequest {
    private String title;
    private String body;
    private Map<String, Object> data;
    private Boolean read;
}
