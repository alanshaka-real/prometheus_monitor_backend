package com.wenmin.prometheus.module.alert.notification;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class EmailSender implements NotificationSender {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    public String getType() {
        return "email";
    }

    @Override
    public boolean send(Map<String, String> config, String message) {
        try {
            String to = config.get("to");
            String subject = config.getOrDefault("subject", "Prometheus Alert");

            if (mailSender == null) {
                log.info("JavaMailSender not configured, falling back to logging - to: {}, subject: {}, message: {}", to, subject, message);
                return true;
            }

            String cc = config.get("cc");
            String from = config.getOrDefault("from", "prometheus-monitor@example.com");

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to.split(","));
            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc.split(","));
            }
            helper.setSubject(subject);
            helper.setText(message, true);

            mailSender.send(mimeMessage);
            log.info("Email sent successfully - to: {}, subject: {}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("Email send failed", e);
            return false;
        }
    }
}
