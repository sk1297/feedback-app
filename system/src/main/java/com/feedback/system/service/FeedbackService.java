package com.feedback.system.service;

import com.feedback.system.entity.Feedback;
import com.feedback.system.repository.FeedbackRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public Feedback saveFeedback(Feedback feedback) {
        try {
            return feedbackRepository.save(feedback);
        } catch (Exception e) {
            throw new RuntimeException("Error saving feedback: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAllOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Feedback> getTodaysFeedback() {
        return feedbackRepository.findTodaysFeedback();
    }

    @Transactional(readOnly = true)
    public Long getTodaysFeedbackCount() {
        return feedbackRepository.getTodaysFeedbackCount();
    }

    @Transactional(readOnly = true)
    public Long getWeeklyFeedbackCount() {
        LocalDateTime weekStart = LocalDateTime.now().minusWeeks(1);
        return feedbackRepository.countByCreatedAtAfter(weekStart);
    }

    @Transactional(readOnly = true)
    public Long getMonthlyFeedbackCount() {
        LocalDateTime monthStart = LocalDateTime.now().minusMonths(1);
        return feedbackRepository.countByCreatedAtAfter(monthStart);
    }

    @Transactional(readOnly = true)
    public List<Feedback> getFeedbackByPhoneNumber(String phoneNumber) {
        return feedbackRepository.findByPhoneNumberOrderByCreatedAtDesc(phoneNumber);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getFeedbackStatistics() {
        List<Object[]> results = feedbackRepository.getFeedbackCountByEmoji();
        Map<String, Long> statistics = new HashMap<>();

        for (Object[] result : results) {
            String emoji = (String) result[0];
            Long count = (Long) result[1];
            statistics.put(emoji, count);
        }

        return statistics;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getDailyFeedbackTrend(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Feedback> recentFeedback = feedbackRepository.findByCreatedAtAfterOrderByCreatedAtAsc(startDate);

        Map<String, Long> dailyTrend = new LinkedHashMap<>();

        // Initialize all days with 0 count
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dailyTrend.put(date.format(DATE_FORMATTER), 0L);
        }

        // Count feedback per day
        Map<LocalDate, Long> feedbackCount = recentFeedback.stream()
                .collect(Collectors.groupingBy(
                        feedback -> feedback.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // Update counts
        feedbackCount.forEach((date, count) -> {
            dailyTrend.put(date.format(DATE_FORMATTER), count);
        });

        return dailyTrend;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getMonthlyFeedbackTrend(int months) {
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
        List<Feedback> recentFeedback = feedbackRepository.findByCreatedAtAfterOrderByCreatedAtAsc(startDate);

        Map<String, Long> monthlyTrend = new LinkedHashMap<>();

        // Initialize all months with 0 count
        for (int i = months - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusMonths(i);
            String monthYear = date.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            monthlyTrend.put(monthYear, 0L);
        }

        // Count feedback per month
        Map<String, Long> feedbackCount = recentFeedback.stream()
                .collect(Collectors.groupingBy(
                        feedback -> feedback.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy")),
                        Collectors.counting()
                ));

        // Update counts
        feedbackCount.forEach((monthYear, count) -> {
            if (monthlyTrend.containsKey(monthYear)) {
                monthlyTrend.put(monthYear, count);
            }
        });

        return monthlyTrend;
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> getHourlyFeedbackDistribution() {
        List<Feedback> allFeedback = feedbackRepository.findAll();

        Map<Integer, Long> hourlyDistribution = new HashMap<>();

        // Initialize all hours with 0 count
        for (int i = 0; i < 24; i++) {
            hourlyDistribution.put(i, 0L);
        }

        // Count feedback per hour
        Map<Integer, Long> feedbackCount = allFeedback.stream()
                .collect(Collectors.groupingBy(
                        feedback -> feedback.getCreatedAt().getHour(),
                        Collectors.counting()
                ));

        // Update counts
        feedbackCount.forEach((hour, count) -> {
            hourlyDistribution.put(hour, count);
        });

        return hourlyDistribution;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopFeedbackContributors(int limit) {
        List<Object[]> results = feedbackRepository.findTopContributors(limit);
        List<Map<String, Object>> contributors = new ArrayList<>();

        for (Object[] result : results) {
            Map<String, Object> contributor = new HashMap<>();
            contributor.put("phoneNumber", result[0]);
            contributor.put("feedbackCount", result[1]);
            contributor.put("lastFeedbackDate", result[2]);
            contributors.add(contributor);
        }

        return contributors;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSatisfactionMetrics() {
        Map<String, Long> ratingStats = getFeedbackStatistics();
        Map<String, Object> metrics = new HashMap<>();

        long totalFeedback = ratingStats.values().stream().mapToLong(Long::longValue).sum();

        if (totalFeedback == 0) {
            metrics.put("satisfactionRate", 0.0);
            metrics.put("averageRating", 0.0);
            metrics.put("positiveCount", 0L);
            metrics.put("negativeCount", 0L);
            metrics.put("neutralCount", 0L);
            return metrics;
        }

        // Define rating mappings
        long positiveCount = ratingStats.getOrDefault("😊", 0L) + ratingStats.getOrDefault("😍", 0L);
        long negativeCount = ratingStats.getOrDefault("😡", 0L) + ratingStats.getOrDefault("😞", 0L);
        long neutralCount = ratingStats.getOrDefault("😐", 0L);

        double satisfactionRate = (double) positiveCount / totalFeedback * 100;

        // Calculate weighted average (1-5 scale)
        double weightedSum =
                ratingStats.getOrDefault("😡", 0L) * 1 +
                        ratingStats.getOrDefault("😞", 0L) * 2 +
                        ratingStats.getOrDefault("😐", 0L) * 3 +
                        ratingStats.getOrDefault("😊", 0L) * 4 +
                        ratingStats.getOrDefault("😍", 0L) * 5;

        double averageRating = weightedSum / totalFeedback;

        metrics.put("satisfactionRate", Math.round(satisfactionRate * 100.0) / 100.0);
        metrics.put("averageRating", Math.round(averageRating * 100.0) / 100.0);
        metrics.put("positiveCount", positiveCount);
        metrics.put("negativeCount", negativeCount);
        metrics.put("neutralCount", neutralCount);
        metrics.put("totalFeedback", totalFeedback);

        return metrics;
    }

    @Transactional(readOnly = true)
    public List<Feedback> getRecentFeedback(int limit) {
        return feedbackRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public byte[] generateExcelReport(List<Feedback> feedbackList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Feedback Report");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            // Create data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"S.No", "Phone Number", "Emoji Rating", "Feedback Text", "Date & Time"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (Feedback feedback : feedbackList) {
                Row row = sheet.createRow(rowNum);

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum);
                cell0.setCellStyle(dataStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(feedback.getPhoneNumber());
                cell1.setCellStyle(dataStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(feedback.getEmoji());
                cell2.setCellStyle(dataStyle);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(feedback.getFeedbackText() != null ? feedback.getFeedbackText() : "No additional comments");
                cell3.setCellStyle(dataStyle);

                Cell cell4 = row.createCell(4);
                cell4.setCellValue(feedback.getCreatedAt().format(FORMATTER));
                cell4.setCellStyle(dataStyle);

                rowNum++;
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }
}