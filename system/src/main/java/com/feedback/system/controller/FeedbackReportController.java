package com.feedback.system.controller;

import com.feedback.system.component.ScheduledTasks;
import com.feedback.system.entity.Feedback;
import com.feedback.system.service.EmailService;
import com.feedback.system.service.FeedbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback-reports")
public class FeedbackReportController {
    
    private static final Logger logger = LoggerFactory.getLogger(FeedbackReportController.class);
    
    @Autowired
    private ScheduledTasks scheduledTasks;
    
    @Autowired
    private FeedbackService feedbackService;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Manually trigger the daily feedback report
     * GET /api/feedback-reports/trigger-daily-report
     */
    @GetMapping("/trigger-daily-report")
    public ResponseEntity<Map<String, Object>> triggerDailyReport() {
        logger.info("Manual trigger for daily feedback report initiated via API");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Call the scheduled task method directly
            scheduledTasks.sendDailyFeedbackReport();
            
            response.put("success", true);
            response.put("message", "Daily feedback report triggered successfully");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error triggering daily feedback report via API: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error occurred while triggering report: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get today's feedback count without sending email
     * GET /api/feedback-reports/today-count
     */
    @GetMapping("/today-count")
    public ResponseEntity<Map<String, Object>> getTodaysFeedbackCount() {
        logger.info("API request for today's feedback count");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            int count = Math.toIntExact(feedbackService.getTodaysFeedbackCount());
            
            response.put("success", true);
            response.put("todaysFeedbackCount", count);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting today's feedback count via API: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error occurred while getting feedback count: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Generate and send custom feedback report for specific date range
     * POST /api/feedback-reports/send-custom-report
     */
    @PostMapping("/send-custom-report")
    public ResponseEntity<Map<String, Object>> sendCustomReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        logger.info("API request for custom feedback report. StartDate: {}, EndDate: {}", startDate, endDate);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Feedback> feedbackList;
            
            // If no date range provided, use today's feedback
            if (startDate == null && endDate == null) {
                feedbackList = feedbackService.getTodaysFeedback();
            } else {
                // You'll need to implement this method in FeedbackService
                // feedbackList = feedbackService.getFeedbackByDateRange(startDate, endDate);
                feedbackList = feedbackService.getTodaysFeedback(); // Fallback for now
            }
            
            if (!feedbackList.isEmpty()) {
                byte[] excelData = feedbackService.generateExcelReport(feedbackList);
                emailService.sendDailyReport(excelData, feedbackList.size());
                
                response.put("success", true);
                response.put("message", "Custom feedback report sent successfully");
                response.put("feedbackCount", feedbackList.size());
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "No feedback found for the specified criteria");
                response.put("feedbackCount", 0);
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            logger.error("Error sending custom feedback report via API: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error occurred while sending custom report: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get feedback report status/health check
     * GET /api/feedback-reports/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getReportStatus() {
        logger.info("API request for feedback report status");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            int todayCount = Math.toIntExact(feedbackService.getTodaysFeedbackCount());
            
            response.put("success", true);
            response.put("status", "healthy");
            response.put("todaysFeedbackCount", todayCount);
            response.put("schedulerEnabled", true);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting feedback report status via API: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}