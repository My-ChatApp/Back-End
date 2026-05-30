package iuh.fit.chatservice.dto.request;

import lombok.Data;

@Data
public class MessageAttachmentRequest {
    private String attachmentId;
    private String fileType;
    private String mimeType;
    private String url;
    private String s3Key;
    private String fileName;
    private Long size;
    private Integer width;
    private Integer height;
    private Integer duration;
    private String thumbnailUrl;
}
