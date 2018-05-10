package org.mewx.github.collector.util;

import java.util.Base64;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static org.mewx.github.collector.Constants.*;

public class MailSender {

    public static void main(String[] args) {
        send(MAIL_NAME, MAIL_SUBJECT, "testing mail");
    }

    public static void send(String to, String subject, String content) {
        final String username = "mseopt@gmail.com";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, new String(Base64.getDecoder().decode(Base64.getDecoder().decode(Base64.getDecoder().decode("WWxoT2JHSXpRakJqUjBaNll6TmtkbU50VVQwPQ==".getBytes())))));
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);
            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
