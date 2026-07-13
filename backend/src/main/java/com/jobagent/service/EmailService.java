package com.jobagent.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from:noreply@jobagent.com}")
    private String fromAddress;

    @Value("${app.mail.frontend-url:http://localhost:3002}")
    private String frontendUrl;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Async
    public void sendNotificationEmail(String to, String subject, String htmlBody) {
        if (!isEmailEnabled()) {
            log.debug("Email not enabled, skipping send to {}", to);
            return;
        }
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            javaMailSender.send(message);
            log.info("Notification email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send notification email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendWeeklySummary(String to, Map<String, Object> summaryData) {
        if (!isEmailEnabled()) {
            log.debug("Email not enabled, skipping weekly summary to {}", to);
            return;
        }
        String subject = "Your Weekly Job Search Summary";
        String htmlBody = buildWeeklySummaryHtml(summaryData);
        sendNotificationEmail(to, subject, htmlBody);
    }

    @Async
    public void sendDailySummary(String to, Map<String, Object> summaryData) {
        if (!isEmailEnabled()) {
            log.debug("Email not enabled, skipping daily summary to {}", to);
            return;
        }
        String subject = "Your Daily Job Search Summary";
        String htmlBody = buildDailySummaryHtml(summaryData);
        sendNotificationEmail(to, subject, htmlBody);
    }

    @Async
    public void sendPasswordResetEmail(String to, String resetToken) {
        if (!isEmailEnabled()) {
            log.debug("Email not enabled, skipping password reset to {}", to);
            return;
        }
        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "Reset Your Password";
        String htmlBody = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2563eb;">Password Reset Request</h2>
                    <p>You requested a password reset for your Job Agent account.</p>
                    <p>Click the button below to reset your password:</p>
                    <p style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;">Reset Password</a>
                    </p>
                    <p style="color: #666; font-size: 14px;">This link will expire in 1 hour.</p>
                    <p style="color: #666; font-size: 14px;">If you did not request this, you can safely ignore this email.</p>
                </body>
                </html>
                """.formatted(resetUrl);
        sendNotificationEmail(to, subject, htmlBody);
    }

    public boolean isEmailEnabled() {
        return mailHost != null && !mailHost.isBlank();
    }

    private String buildWeeklySummaryHtml(Map<String, Object> data) {
        int applicationsSent = data.containsKey("applicationsSent") ? (int) data.get("applicationsSent") : 0;
        int interviewsScheduled = data.containsKey("interviewsScheduled") ? (int) data.get("interviewsScheduled") : 0;
        int responsesReceived = data.containsKey("responsesReceived") ? (int) data.get("responsesReceived") : 0;
        String weekLabel = data.containsKey("weekLabel") ? (String) data.get("weekLabel") : "This Week";

        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2563eb;">Weekly Job Search Summary</h2>
                    <p style="color: #666;">%s</p>
                    <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                        <tr style="background-color: #f8fafc;">
                            <td style="padding: 12px; border: 1px solid #e2e8f0; font-weight: bold;">Applications Sent</td>
                            <td style="padding: 12px; border: 1px solid #e2e8f0; text-align: center; color: #2563eb; font-weight: bold;">%d</td>
                        </tr>
                        <tr>
                            <td style="padding: 12px; border: 1px solid #e2e8f0; font-weight: bold;">Interviews Scheduled</td>
                            <td style="padding: 12px; border: 1px solid #e2e8f0; text-align: center; color: #16a34a; font-weight: bold;">%d</td>
                        </tr>
                        <tr style="background-color: #f8fafc;">
                            <td style="padding: 12px; border: 1px solid #e2e8f0; font-weight: bold;">Responses Received</td>
                            <td style="padding: 12px; border: 1px solid #e2e8f0; text-align: center; color: #ca8a04; font-weight: bold;">%d</td>
                        </tr>
                    </table>
                    <p><a href="%s/dashboard" style="color: #2563eb;">View Full Dashboard</a></p>
                </body>
                </html>
                """.formatted(weekLabel, applicationsSent, interviewsScheduled, responsesReceived, frontendUrl);
    }

    private String buildDailySummaryHtml(Map<String, Object> data) {
        int newJobs = data.containsKey("newJobs") ? (int) data.get("newJobs") : 0;
        int applicationsSent = data.containsKey("applicationsSent") ? (int) data.get("applicationsSent") : 0;
        int responses = data.containsKey("responses") ? (int) data.get("responses") : 0;
        String dateLabel = data.containsKey("dateLabel") ? (String) data.get("dateLabel") : "Today";

        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2563eb;">Daily Job Search Summary</h2>
                    <p style="color: #666;">%s</p>
                    <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                        <tr style="background-color: #f8fafc;">
                            <td style="padding: 12px; border: 1px solid #e2e8f0; font-weight: bold;">New Matching Jobs</td>
                            <td style="padding: 12px; border: 1px solid #e2e8f0; text-align: center; color: #2563eb; font-weight: bold;">%d</td>
                        </tr>
                        <tr>
                            <td style="padding: 12px; border: 1px solid #e2e8f0; font-weight: bold;">Applications Sent</td>
                            <td style="padding: 12px; border: 1px solid #e2e8f0; text-align: center; color: #16a34a; font-weight: bold;">%d</td>
                        </tr>
                        <tr style="background-color: #f8fafc;">
                            <td style="padding: 12px; border: 1px solid #e2e8f0; font-weight: bold;">Responses</td>
                            <td style="padding: 12px; border: 1px solid #e2e8f0; text-align: center; color: #ca8a04; font-weight: bold;">%d</td>
                        </tr>
                    </table>
                    <p><a href="%s/dashboard" style="color: #2563eb;">View Full Dashboard</a></p>
                </body>
                </html>
                """.formatted(dateLabel, newJobs, applicationsSent, responses, frontendUrl);
    }
}
