package iuh.fit.chatservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachmentDto {
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
