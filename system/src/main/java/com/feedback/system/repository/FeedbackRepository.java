package com.feedback.system.repository;

import com.feedback.system.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // Find feedback between two dates
    List<Feedback> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Find all feedback ordered by creation date (newest first)
    @Query("SELECT f FROM Feedback f ORDER BY f.createdAt DESC")
    List<Feedback> findAllOrderByCreatedAtDesc();

    // Find today's feedback - PostgreSQL compatible
    @Query("SELECT f FROM Feedback f WHERE DATE_TRUNC('day', f.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP) ORDER BY f.createdAt DESC")
    List<Feedback> findTodaysFeedback();

    // Find feedback by phone number
    List<Feedback> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    // Find feedback by emoji
    List<Feedback> findByEmojiOrderByCreatedAtDesc(String emoji);

    // Get feedback count for today - PostgreSQL compatible
    @Query("SELECT COUNT(f) FROM Feedback f WHERE DATE_TRUNC('day', f.createdAt) = DATE_TRUNC('day', CURRENT_TIMESTAMP)")
    Long getTodaysFeedbackCount();

    // Get feedback count by emoji
    @Query("SELECT f.emoji, COUNT(f) FROM Feedback f GROUP BY f.emoji")
    List<Object[]> getFeedbackCountByEmoji();

    // New methods for analytics
    Long countByCreatedAtAfter(LocalDateTime dateTime);

    List<Feedback> findByCreatedAtAfterOrderByCreatedAtAsc(LocalDateTime dateTime);

    @Query("SELECT f.phoneNumber, COUNT(f), MAX(f.createdAt) FROM Feedback f " +
            "GROUP BY f.phoneNumber ORDER BY COUNT(f) DESC")
    List<Object[]> findTopContributors(@Param("limit") int limit);

    @Query("SELECT f FROM Feedback f ORDER BY f.createdAt DESC LIMIT 5")
    List<Feedback> findTop5ByOrderByCreatedAtDesc();

    // Additional useful queries for analytics - PostgreSQL compatible
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.createdAt >= :startDate AND f.createdAt <= :endDate")
    Long countFeedbackBetweenDates(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    // PostgreSQL uses EXTRACT instead of HOUR() function
    @Query("SELECT EXTRACT(HOUR FROM f.createdAt), COUNT(f) FROM Feedback f GROUP BY EXTRACT(HOUR FROM f.createdAt) ORDER BY EXTRACT(HOUR FROM f.createdAt)")
    List<Object[]> getFeedbackCountByHour();

    // PostgreSQL uses DATE_TRUNC instead of DATE() function
    @Query("SELECT DATE_TRUNC('day', f.createdAt), COUNT(f) FROM Feedback f " +
            "WHERE f.createdAt >= :startDate GROUP BY DATE_TRUNC('day', f.createdAt) ORDER BY DATE_TRUNC('day', f.createdAt)")
    List<Object[]> getDailyFeedbackCount(@Param("startDate") LocalDateTime startDate);

    // PostgreSQL uses EXTRACT for month and year
    @Query("SELECT EXTRACT(MONTH FROM f.createdAt), EXTRACT(YEAR FROM f.createdAt), COUNT(f) FROM Feedback f " +
            "WHERE f.createdAt >= :startDate GROUP BY EXTRACT(MONTH FROM f.createdAt), EXTRACT(YEAR FROM f.createdAt) " +
            "ORDER BY EXTRACT(YEAR FROM f.createdAt), EXTRACT(MONTH FROM f.createdAt)")
    List<Object[]> getMonthlyFeedbackCount(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT f FROM Feedback f WHERE f.feedbackText IS NOT NULL AND f.feedbackText != '' " +
            "ORDER BY f.createdAt DESC")
    List<Feedback> findFeedbackWithComments();

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.feedbackText IS NOT NULL AND f.feedbackText != ''")
    Long countFeedbackWithComments();
}