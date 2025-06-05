package com.feedback.system.restcontroller;

import com.feedback.system.entity.Feedback;
import com.feedback.system.service.FeedbackService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackRestController {

    private final FeedbackService feedbackService;
    private final Logger logger = LoggerFactory.getLogger(FeedbackRestController.class);

    @Autowired
    public FeedbackRestController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<?> submitFeedback(@Valid @RequestBody Feedback feedback,
                                            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", "Validation failed",
                    "errors", bindingResult.getFieldErrors().stream().map(fieldError -> Map.of(
                            "field", fieldError.getField(),
                            "message", fieldError.getDefaultMessage()
                    )).toList()
            ));
        }

        try {
            Feedback savedFeedback = feedbackService.saveFeedback(feedback);
            logger.info("Feedback submitted successfully. ID: {}, Phone: {}", 
                        savedFeedback.getId(), savedFeedback.getPhoneNumber());

            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Thank you for your valuable feedback! 🎉",
                    "data", savedFeedback
            ));
        } catch (Exception e) {
            logger.error("Error submitting feedback: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "message", "Sorry, there was an error submitting your feedback. Please try again."
            ));
        }
    }
}
