package com.feedback.system.component;

import com.feedback.system.entity.Feedback;
import com.feedback.system.service.EmailService;
import com.feedback.system.service.FeedbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ScheduledTasks {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    
    @Autowired
    private FeedbackService feedbackService;
    
    @Autowired
    private EmailService emailService;
    
    // Runs every day at 11 PM (23:00)
    @Scheduled(cron = "0/30 * * * * ?")
    public void sendDailyFeedbackReport() {
        logger.info("Starting daily feedback report generation...");
        
        try {
            List<Feedback> todaysFeedback = feedbackService.getTodaysFeedback();
            
            if (!todaysFeedback.isEmpty()) {
                byte[] excelData = feedbackService.generateExcelReport(todaysFeedback);
                emailService.sendDailyReport(excelData, todaysFeedback.size());
                logger.info("Daily feedback report sent successfully! Total feedback: {}", todaysFeedback.size());
            } else {
                logger.info("No feedback received today. Email not sent.");
            }
        } catch (Exception e) {
            logger.error("Error occurred while sending daily feedback report: {}", e.getMessage(), e);
        }
    }
    
    // Optional: Test method that runs every minute (for testing purposes)
    // Remove this in production
    // @Scheduled(fixedRate = 60000)
    public void testScheduler() {
        logger.debug("Scheduler is running... Current feedback count: {}", 
                    feedbackService.getTodaysFeedbackCount());
    }
}