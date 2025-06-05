package com.feedback.system.controller;

import com.feedback.system.entity.Feedback;
import com.feedback.system.service.FeedbackService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
public class FeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/")
    public String feedbackForm(Model model) {
        model.addAttribute("feedback", new Feedback());
        logger.info("Feedback form page accessed");
        return "feedback-form";
    }

    @PostMapping("/feedback")
    public String submitFeedback(@Valid @ModelAttribute Feedback feedback,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please correct the errors and try again.");
            return "redirect:/";
        }

        try {
            Feedback savedFeedback = feedbackService.saveFeedback(feedback);
            logger.info("Feedback submitted successfully. ID: {}, Phone: {}",
                    savedFeedback.getId(), savedFeedback.getPhoneNumber());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Thank you for your valuable feedback! 🎉");
        } catch (Exception e) {
            logger.error("Error submitting feedback: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sorry, there was an error submitting your feedback. Please try again.");
        }

        return "redirect:/";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            List<Feedback> feedbackList = feedbackService.getAllFeedback();
            Long todaysCount = feedbackService.getTodaysFeedbackCount();
            Map<String, Long> statistics = feedbackService.getFeedbackStatistics();

            model.addAttribute("feedbackList", feedbackList);
            model.addAttribute("totalCount", feedbackList.size());
            model.addAttribute("todaysCount", todaysCount);
            model.addAttribute("statistics", statistics);

            logger.info("Dashboard accessed. Total feedback: {}, Today's count: {}",
                    feedbackList.size(), todaysCount);
        } catch (Exception e) {
            logger.error("Error loading dashboard: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading dashboard data");
        }

        return "dashboard";
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        try {
            // Basic statistics
            List<Feedback> allFeedback = feedbackService.getAllFeedback();
            Long todaysCount = feedbackService.getTodaysFeedbackCount();
            Long weeklyCount = feedbackService.getWeeklyFeedbackCount();
            Long monthlyCount = feedbackService.getMonthlyFeedbackCount();

            // Rating distribution
            Map<String, Long> ratingDistribution = feedbackService.getFeedbackStatistics();

            // Trend data (last 7 days, 30 days)
            Map<String, Long> dailyTrend = feedbackService.getDailyFeedbackTrend(7);
            Map<String, Long> monthlyTrend = feedbackService.getMonthlyFeedbackTrend(12);

            // Peak hours analysis
            Map<Integer, Long> hourlyDistribution = feedbackService.getHourlyFeedbackDistribution();

            // Top feedback contributors
            List<Map<String, Object>> topContributors = feedbackService.getTopFeedbackContributors(10);

            // Satisfaction metrics
            Map<String, Object> satisfactionMetrics = feedbackService.getSatisfactionMetrics();

            // Recent feedback summary
            List<Feedback> recentFeedback = feedbackService.getRecentFeedback(5);

            model.addAttribute("totalCount", allFeedback.size());
            model.addAttribute("todaysCount", todaysCount);
            model.addAttribute("weeklyCount", weeklyCount);
            model.addAttribute("monthlyCount", monthlyCount);
            model.addAttribute("ratingDistribution", ratingDistribution);
            model.addAttribute("dailyTrend", dailyTrend);
            model.addAttribute("monthlyTrend", monthlyTrend);
            model.addAttribute("hourlyDistribution", hourlyDistribution);
            model.addAttribute("topContributors", topContributors);
            model.addAttribute("satisfactionMetrics", satisfactionMetrics);
            model.addAttribute("recentFeedback", recentFeedback);

            logger.info("Analytics page accessed. Total feedback analyzed: {}", allFeedback.size());
        } catch (Exception e) {
            logger.error("Error loading analytics: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading analytics data");
        }

        return "analytics";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel() {
        try {
            List<Feedback> feedbackList = feedbackService.getAllFeedback();
            byte[] excelData = feedbackService.generateExcelReport(feedbackList);

            String fileName = "Feedback_Report_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) +
                    ".xlsx";

            logger.info("Excel export requested. Total records: {}", feedbackList.size());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelData);

        } catch (Exception e) {
            logger.error("Error generating Excel export: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public String searchFeedback(@RequestParam(required = false) String phoneNumber, Model model) {
        try {
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                List<Feedback> searchResults = feedbackService.getFeedbackByPhoneNumber(phoneNumber.trim());
                model.addAttribute("feedbackList", searchResults);
                model.addAttribute("searchQuery", phoneNumber);
                model.addAttribute("totalCount", searchResults.size());

                logger.info("Search performed for phone number: {}. Results: {}",
                        phoneNumber, searchResults.size());
            } else {
                return "redirect:/dashboard";
            }
        } catch (Exception e) {
            logger.error("Error searching feedback: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error performing search");
        }

        return "dashboard";
    }

    // API endpoints for AJAX calls from analytics page
    @GetMapping("/api/analytics/rating-distribution")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getRatingDistribution() {
        try {
            Map<String, Long> distribution = feedbackService.getFeedbackStatistics();
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            logger.error("Error fetching rating distribution: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/analytics/daily-trend")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getDailyTrend(@RequestParam(defaultValue = "7") int days) {
        try {
            Map<String, Long> trend = feedbackService.getDailyFeedbackTrend(days);
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            logger.error("Error fetching daily trend: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/analytics/hourly-distribution")
    @ResponseBody
    public ResponseEntity<Map<Integer, Long>> getHourlyDistribution() {
        try {
            Map<Integer, Long> distribution = feedbackService.getHourlyFeedbackDistribution();
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            logger.error("Error fetching hourly distribution: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}