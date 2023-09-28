package pl.databucket.server.service.mail;

import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import pl.databucket.server.entity.User;

import javax.mail.MessagingException;

@Service
@AllArgsConstructor
public class MailSenderService {

    private JavaMailSender sender;

    public void sendConfirmationLink(User user, String link, String from) throws MessagingException {
        String htmlContent = "<h2>Thanks for signing up to Databucket.</h2>";

        htmlContent += "<p>To confirm your registration, copy and paste the URL into your browser:</p>";
        htmlContent += "<h4>" + link + "</h4><br/>";

        htmlContent += "<p style=\"color:red;\">Please note that the link will expire in 48 hours.</p>";
        htmlContent += "<p>If you didn't request a registration, don't worry. You can safely ignore this email.</p><br/><br/>";

        MailSenderHandler mailHandler = new MailSenderHandler(sender);
        if (from != null) {
            mailHandler.setFrom(from);
        }
        mailHandler.setTo(user.getEmail());
        mailHandler.setSubject("Databucket account - registration");
        mailHandler.setText(htmlContent, true);
        mailHandler.send();
    }

    public void sendRegistrationConfirmation(User user, String from) throws MessagingException {
        String htmlContent = "<h2>Databucket account</h2>";

        htmlContent += "<p>Your account has been activated.</p>";
        htmlContent += "<p>Log in to the application and request to be assigned to the project.</p>";

        MailSenderHandler mailHandler = new MailSenderHandler(sender);
        if (from != null) {
            mailHandler.setFrom(from);
        }
        mailHandler.setTo(user.getEmail());
        mailHandler.setSubject("Databucket account - activated");
        mailHandler.setText(htmlContent, true);
        mailHandler.send();
    }

    public void sendForgotPasswordLink(User user, String link, String from) throws MessagingException {
        String htmlContent = "<h2>Databucket account</h2>";
        htmlContent += "<h4>Forgot your password?</h4>";
        htmlContent += "<p>We received a request to reset the password for your account.</p><br/>";

        htmlContent += "<p>To reset your password, copy and paste the URL into your browser:</p>";
        htmlContent += "<h4>" + link + "</h4><br/>";

        htmlContent += "<p style=\"color:red;\">Please note that the link will expire in 48 hours.</p>";
        htmlContent += "<p>If you didn't request a reset, don't worry. You can safely ignore this email.</p><br/><br/>";

        MailSenderHandler mailHandler = new MailSenderHandler(sender);
        if (from != null) {
            mailHandler.setFrom(from);
        }
        mailHandler.setTo(user.getEmail());
        mailHandler.setSubject("Databucket account - forgot your password?");
        mailHandler.setText(htmlContent, true);
        mailHandler.send();
    }

    public void sendNewPassword(User user, String password, String from) throws MessagingException {
        String htmlContent = "<h2>Databucket account</h2>";
        htmlContent += "<p>Your password has been reset.</p>";
        htmlContent += "<p>Below you can find your temporary password:</p>";
        htmlContent += "<h2>" + password + "</h2>";

        MailSenderHandler mailHandler = new MailSenderHandler(sender);
        if (from != null) {
            mailHandler.setFrom(from);
        }
        mailHandler.setTo(user.getEmail());
        mailHandler.setSubject("Databucket account - temporary password");
        mailHandler.setText(htmlContent, true);
        mailHandler.send();
    }


}
