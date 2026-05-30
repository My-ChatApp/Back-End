package iuh.fit.notificationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Service
@RequiredArgsConstructor
public class SesMailService {

    private final SesV2Client sesClient;

    @Value("${mychatapp.mail.from:}")
    private String mailFrom;

    @Value("${mychatapp.mail.from-name:MyChatApp}")
    private String mailFromName;

    public void sendText(String to, String subject, String body) {
        if (!StringUtils.hasText(mailFrom)) {
            throw new IllegalStateException("MAIL_FROM chưa cấu hình — cần email thuộc domain đã verify trên SES");
        }

        String fromAddress = StringUtils.hasText(mailFromName)
                ? mailFromName + " <" + mailFrom + ">"
                : mailFrom;

        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(fromAddress)
                .destination(Destination.builder().toAddresses(to).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data(subject).charset("UTF-8").build())
                                .body(Body.builder()
                                        .text(Content.builder().data(body).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
    }
}
