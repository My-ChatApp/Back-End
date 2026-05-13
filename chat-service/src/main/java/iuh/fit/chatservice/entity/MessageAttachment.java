package iuh.fit.chatservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "message_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageAttachment {

    @Id
    private String id;

    private Message message;

    private String url;

    private String fileName;

    private String fileType;

    private Long size;

    private Integer width;

    private Integer height;

    private Integer duration;
}