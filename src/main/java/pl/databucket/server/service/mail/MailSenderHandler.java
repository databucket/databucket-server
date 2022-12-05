package pl.databucket.server.service.mail;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;

public class MailSenderHandler {

    private JavaMailSender sender;
    private MimeMessage message;
    private MimeMessageHelper messageHelper;

    public MailSenderHandler(JavaMailSender sender) throws MessagingException {
        this.sender = sender;
        message = sender.createMimeMessage();
        messageHelper = new MimeMessageHelper(message, true, "UTF-8");
    }

    public void setFrom(String fromAddress) throws MessagingException {
        messageHelper.setFrom(fromAddress);
    }

    public void setTo(String toAddress) throws MessagingException {
        messageHelper.setTo(toAddress);
    }

    public void setSubject(String subject) throws MessagingException {
        messageHelper.setSubject(subject);
    }

    public void setText(String text, boolean useHtml) throws MessagingException {
        messageHelper.setText(text, useHtml);
    }

    public void setAttach(String displayFileName, MultipartFile file) throws MessagingException {
        messageHelper.addAttachment(displayFileName, file);
    }

    public void setInline(String contentId, MultipartFile file) throws MessagingException, IOException {
        messageHelper.addInline(contentId, new ByteArrayResource(file.getBytes()), "image/jpeg");
    }

    public void send() {
        sender.send(message);
    }
}
