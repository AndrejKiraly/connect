package com.andrejKir.connect.accounts.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetMailer {
    private final JavaMailSender mailSender;
    private final  String from;
    private final  String linkBase;


    public PasswordResetMailer(JavaMailSender mailSender, @Value("${app.mail.from}") String from,@Value("${app.password-reset.link-base}") String linkBase) {
        this.mailSender = mailSender;
        this.from = from;
        this.linkBase = linkBase;
    }

    public void sendResetLink(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Reset your password");
        message.setText("Reset link: " + linkBase + "?token=" + token
                + "\nThe link expires in 15 minutes.");
        mailSender.send(message);
    }
}
