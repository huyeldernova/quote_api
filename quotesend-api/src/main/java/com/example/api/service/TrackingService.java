package com.example.api.service;

import com.example.api.entity.EmailLog;
import com.example.api.entity.Quote;
import com.example.api.enums.QuoteStatus;
import com.example.api.repository.EmailLogRepository;
import com.example.api.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service @RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final EmailLogRepository emailLogRepository;
    private final QuoteRepository    quoteRepository;

    // 1x1 transparent GIF bytes
    private static final byte[] TRANSPARENT_GIF = java.util.Base64.getDecoder().decode(
            "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    @Transactional
    public byte[] trackOpen(String token) {
        Optional<EmailLog> opt = emailLogRepository.findByTrackingToken(token);
        if (opt.isPresent()) {
            EmailLog log = opt.get();
            if (Boolean.FALSE.equals(log.getOpened())) {
                log.setOpened(true);
                log.setOpenedAt(LocalDateTime.now());
                emailLogRepository.save(log);
                // Update quote status to VIEWED
                Quote quote = log.getQuote();
                if (quote != null && quote.getStatus() == QuoteStatus.SENT) {
                    quote.setStatus(QuoteStatus.VIEWED);
                    quoteRepository.save(quote);
                }

            }
        }
        return TRANSPARENT_GIF;
    }

    @Transactional
    public void trackClick(String token) {
        emailLogRepository.findByTrackingToken(token).ifPresent(log -> {
            if (Boolean.FALSE.equals(log.getClicked())) {
                log.setClicked(true);
                log.setClickedAt(LocalDateTime.now());
                emailLogRepository.save(log);

            }
        });
    }
}
