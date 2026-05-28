package com.example.api.service;

import com.example.api.dto.response.*;
import com.example.api.entity.EmailLog;
import com.example.api.entity.Quote;
import com.example.api.enums.QuoteStatus;
import com.example.api.repository.EmailLogRepository;
import com.example.api.repository.QuoteRepository;
import com.example.api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service @RequiredArgsConstructor
public class DashboardService {

    private final QuoteRepository    quoteRepository;
    private final EmailLogRepository emailLogRepository;
    private final SecurityUtils      securityUtils;

    public DashboardStatsResponse getStats() {
        String userId = securityUtils.getCurrentUserId();

        long totalQuotes = quoteRepository.countByUserIdAndStatus(userId, QuoteStatus.DRAFT)
                + quoteRepository.countByUserIdAndStatus(userId, QuoteStatus.SENT)
                + quoteRepository.countByUserIdAndStatus(userId, QuoteStatus.VIEWED);
        long draftCount  = quoteRepository.countByUserIdAndStatus(userId, QuoteStatus.DRAFT);
        long sentCount   = quoteRepository.countByUserIdAndStatus(userId, QuoteStatus.SENT);
        long viewedCount = quoteRepository.countByUserIdAndStatus(userId, QuoteStatus.VIEWED);

        Double revenue   = quoteRepository.sumTotalAmountByUserId(userId);

        long emailsSent    = emailLogRepository.countByUserId(userId);
        long emailsOpened  = emailLogRepository.countOpenedByUserId(userId);
        double openRate    = emailsSent > 0
                ? Math.round((emailsOpened * 100.0 / emailsSent) * 10) / 10.0 : 0.0;

        List<QuoteListItemResponse> recentQuotes = quoteRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream().limit(5).map(this::toListItem).toList();

        List<EmailLogResponse> recentEmails = emailLogRepository
                .findTop5ByUserId(userId)
                .stream().map(this::toEmailLog).toList();

        return DashboardStatsResponse.builder()
                .totalQuotes(totalQuotes)
                .estimatedRevenue(revenue != null ? revenue : 0.0)
                .draftCount(draftCount)
                .sentCount(sentCount)
                .viewedCount(viewedCount)
                .emailsSent(emailsSent)
                .openRate(openRate)
                .recentQuotes(recentQuotes)
                .recentEmails(recentEmails)
                .build();
    }

    private QuoteListItemResponse toListItem(Quote q) {
        return QuoteListItemResponse.builder()
                .id(q.getId()).quoteNumber(q.getQuoteNumber())
                .clientName(q.getClientName()).tourName(q.getTourName())
                .startDate(q.getStartDate()).paxCount(q.getPaxCount())
                .pricePerPerson(q.getPricePerPerson()).totalAmount(q.getTotalAmount())
                .status(q.getStatus())
                .createdAt(q.getCreatedAt() != null
                        ? q.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : null)
                .build();
    }

    private EmailLogResponse toEmailLog(EmailLog e) {
        return EmailLogResponse.builder()
                .id(e.getId())
                .quoteId(e.getQuote() != null ? e.getQuote().getId() : null)
                .toEmail(e.getToEmail())
                .ccEmail(e.getCcEmail())
                .subject(e.getSubject())
                .sentAt(e.getSentAt() != null
                        ? e.getSentAt().format(DateTimeFormatter.ofPattern("dd MMM, HH:mm")) : null)
                .opened(e.getOpened())
                .openedAt(e.getOpenedAt() != null
                        ? e.getOpenedAt().format(DateTimeFormatter.ofPattern("dd MMM, HH:mm")) : null)
                .clicked(e.getClicked())
                .clickedAt(e.getClickedAt() != null
                        ? e.getClickedAt().format(DateTimeFormatter.ofPattern("dd MMM, HH:mm")) : null)
                .build();
    }
}
