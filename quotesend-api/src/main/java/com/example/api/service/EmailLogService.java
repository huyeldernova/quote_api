package com.example.api.service;

import com.example.api.dto.response.EmailLogResponse;
import com.example.api.dto.response.EmailStatsResponse;
import com.example.api.dto.response.PageResponse;
import com.example.api.entity.EmailLog;
import com.example.api.exception.AppException;
import com.example.api.exception.ErrorCode;
import com.example.api.repository.EmailLogRepository;
import com.example.api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service @RequiredArgsConstructor
public class EmailLogService {

    private final EmailLogRepository emailLogRepository;
    private final SecurityUtils      securityUtils;

    public PageResponse<EmailLogResponse> getAllLogs(int page, int size) {
        String userId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<EmailLog> result = emailLogRepository.findByUserId(userId, pageable);
        return PageResponse.<EmailLogResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public EmailStatsResponse getStats() {
        String userId = securityUtils.getCurrentUserId();
        long sent    = emailLogRepository.countByUserId(userId);
        long opened  = emailLogRepository.countOpenedByUserId(userId);
        long clicked = emailLogRepository.countClickedByUserId(userId);
        double rate  = sent > 0 ? Math.round((opened * 100.0 / sent) * 10) / 10.0 : 0.0;
        return EmailStatsResponse.builder()
                .totalSent(sent).totalOpened(opened)
                .totalClicked(clicked).openRate(rate).build();
    }

    public EmailLogResponse getById(Long id) {
        String userId = securityUtils.getCurrentUserId();
        EmailLog log = emailLogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_LOG_NOT_FOUND));
        if (log.getQuote() == null || !log.getQuote().getUser().getId().equals(userId))
            throw new AppException(ErrorCode.ACCESS_DENIED);
        return toResponse(log);
    }

    private EmailLogResponse toResponse(EmailLog e) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM, HH:mm");
        return EmailLogResponse.builder()
                .id(e.getId())
                .quoteId(e.getQuote() != null ? e.getQuote().getId() : null)
                .toEmail(e.getToEmail())
                .ccEmail(e.getCcEmail())
                .subject(e.getSubject())
                .sentAt(e.getSentAt()     != null ? e.getSentAt().format(fmt)     : null)
                .opened(e.getOpened())
                .openedAt(e.getOpenedAt() != null ? e.getOpenedAt().format(fmt)   : null)
                .clicked(e.getClicked())
                .clickedAt(e.getClickedAt() != null ? e.getClickedAt().format(fmt) : null)
                .build();
    }
}