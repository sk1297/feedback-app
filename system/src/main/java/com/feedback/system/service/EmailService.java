package com.feedback.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${feedback.email.to}")
    private String toEmail;
    
    @Value("${feedback.email.subject}")
    private String emailSubject;
    
    @Value("${feedback.email.from}")
    private String fromEmail;
    
    public void sendDailyReport(byte[] excelData, int feedbackCount) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(emailSubject + " - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            
            String emailBody = buildEmailBody(feedbackCount);
            helper.setText(emailBody, true);
            
            String fileName = "Feedback_Report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".xlsx";
            helper.addAttachment(fileName, new ByteArrayResource(excelData));
            
            mailSender.send(message);
            logger.info("Daily feedback report sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Error sending daily feedback report: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private String buildEmailBody(int feedbackCount) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                    .header { text-align: center; color: #333; border-bottom: 2px solid #007bff; padding-bottom: 20px; margin-bottom: 30px; }
                    .stats { background: linear-gradient(45deg, #007bff, #0056b3); color: white; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0; }
                    .footer { text-align: center; color: #666; margin-top: 30px; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>📊 Daily Feedback Report</h2>
                        <p>Feedback System - %s</p>
                    </div>
                    
                    <div class="stats">
                        <h3>📈 Today's Statistics</h3>
                        <h1 style="margin: 10px 0; font-size: 48px;">%d</h1>
                        <p>Total Feedback Received</p>
                    </div>
                    
                    <p>Dear Admin,</p>
                    <p>Please find attached the daily feedback report containing all feedback received today.</p>
                    
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <strong>Report Details:</strong><br>
                        📅 Date: %s<br>
                        📊 Total Records: %d<br>
                        📎 File Format: Excel (.xlsx)
                    </div>
                    
                    <p>Thank you for using our Feedback System!</p>
                    
                    <div class="footer">
                        <p>This is an automated email from Feedback Management System</p>
                        <p>© 2025 Feedback System. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
            feedbackCount,
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
            feedbackCount
        );
    }
}