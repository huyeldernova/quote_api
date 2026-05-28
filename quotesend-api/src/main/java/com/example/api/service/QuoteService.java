package com.example.api.service;

import com.example.api.dto.request.QuoteCostRequest;
import com.example.api.dto.request.QuoteDayRequest;
import com.example.api.dto.request.QuoteFormRequest;
import com.example.api.dto.request.SendEmailRequest;
import com.example.api.dto.response.*;
import com.example.api.entity.*;
import com.example.api.enums.QuoteStatus;
import com.example.api.exception.AppException;
import com.example.api.exception.ErrorCode;
import com.example.api.repository.EmailLogRepository;
import com.example.api.repository.QuoteRepository;
import com.example.api.repository.UserRepository;
import com.example.api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class QuoteService {

    private final QuoteRepository    quoteRepository;
    private final EmailLogRepository emailLogRepository;
    private final UserRepository     userRepository;
    private final SecurityUtils      securityUtils;
    private final PdfService         pdfService;
    private final EmailService       emailService;

    // ── READ ──────────────────────────────────────────────────────────────
    public List<QuoteListItemResponse> getAllQuotes() {
        String userId = securityUtils.getCurrentUserId();
        return quoteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toListItem).toList();
    }

    public QuoteResponse getQuote(Long id) {
        String userId = securityUtils.getCurrentUserId();
        return toResponse(findOwned(id, userId));
    }

    // ── CREATE ────────────────────────────────────────────────────────────
    @Transactional
    public QuoteResponse createQuote(QuoteFormRequest req) {
        String userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Quote quote = Quote.builder()
                .quoteNumber(generateQuoteNumber(userId))
                .user(user)
                .status(QuoteStatus.DRAFT)
                .build();

        applyFormFields(quote, req);
        quoteRepository.save(quote);
        return toResponse(quote);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    @Transactional
    public QuoteResponse updateQuote(Long id, QuoteFormRequest req) {
        String userId = securityUtils.getCurrentUserId();
        Quote  quote  = findOwned(id, userId);
        applyFormFields(quote, req);
        quoteRepository.save(quote);
        return toResponse(quote);
    }

    // ── DELETE ────────────────────────────────────────────────────────────
    @Transactional
    public void deleteQuote(Long id) {
        String userId = securityUtils.getCurrentUserId();
        quoteRepository.delete(findOwned(id, userId));
    }

    // ── DUPLICATE ─────────────────────────────────────────────────────────
    @Transactional
    public DuplicateQuoteResponse duplicateQuote(Long id) {
        String userId = securityUtils.getCurrentUserId();
        Quote  src    = findOwned(id, userId);
        User   user   = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Quote copy = Quote.builder()
                .quoteNumber(generateQuoteNumber(userId))
                .user(user)
                .clientName("Copy of " + src.getClientName())
                .clientEmail(src.getClientEmail())
                .tourName(src.getTourName()).tourType(src.getTourType())
                .startDate(src.getStartDate()).endDate(src.getEndDate())
                .routeFrom(src.getRouteFrom()).routeTo(src.getRouteTo())
                .arrivingAt(src.getArrivingAt()).departingFrom(src.getDepartingFrom())
                .transport(src.getTransport()).starRating(src.getStarRating())
                .paxCount(src.getPaxCount()).profitMargin(src.getProfitMargin())
                .pricePerPerson(src.getPricePerPerson()).totalAmount(src.getTotalAmount())
                .status(QuoteStatus.DRAFT)
                .build();

        for (QuoteCost c : src.getCosts()) {
            copy.getCosts().add(QuoteCost.builder().quote(copy)
                    .label(c.getLabel()).amount(c.getAmount()).sortOrder(c.getSortOrder()).build());
        }
        for (QuoteDay d : src.getDays()) {
            copy.getDays().add(QuoteDay.builder().quote(copy)
                    .dayNumber(d.getDayNumber()).location(d.getLocation())
                    .dateLabel(d.getDateLabel()).hotel(d.getHotel())
                    .sights(d.getSights()).note(d.getNote()).imageUrl(d.getImageUrl()).build());
        }
        quoteRepository.save(copy);
        return DuplicateQuoteResponse.builder()
                .newQuoteId(copy.getId()).quoteNumber(copy.getQuoteNumber()).build();
    }

    // ── PDF ───────────────────────────────────────────────────────────────
    public byte[] generatePdf(Long id) {
        return pdfService.generateQuotePdf(findOwned(id, securityUtils.getCurrentUserId()));
    }

    // ── SEND EMAIL ────────────────────────────────────────────────────────
    @Transactional
    public SendEmailResponse sendEmail(Long id, SendEmailRequest req) {
        String userId = securityUtils.getCurrentUserId();
        Quote  quote  = findOwned(id, userId);

        EmailLog emailLog = EmailLog.builder()
                .quote(quote)
                .toEmail(req.getToEmail()).ccEmail(req.getCcEmail())
                .subject(req.getSubject())
                .trackingToken(UUID.randomUUID().toString())
                .sentAt(LocalDateTime.now())
                .opened(false).clicked(false)
                .build();
        emailLogRepository.save(emailLog);

        emailService.sendQuoteEmail(quote, emailLog, req.getMessage());

        quote.setStatus(QuoteStatus.SENT);
        quoteRepository.save(quote);

        return SendEmailResponse.builder().emailLogId(emailLog.getId()).build();
    }

    // ── helpers ───────────────────────────────────────────────────────────
    private Quote findOwned(Long id, String userId) {
        return quoteRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.QUOTE_NOT_FOUND));
    }

    /** Tính giá theo công thức khớp với FE: ceil(cost/(1-margin%)) làm tròn lên 50 */
    private void applyFormFields(Quote q, QuoteFormRequest req) {
        q.setClientName(req.getClientName());
        q.setClientEmail(req.getClientEmail());
        q.setTourName(req.getTourName());
        q.setTourType(req.getTourType());
        q.setStartDate(req.getStartDate());
        q.setEndDate(req.getEndDate());
        q.setRouteFrom(req.getRouteFrom());
        q.setRouteTo(req.getRouteTo());
        q.setArrivingAt(req.getArrivingAt());
        q.setDepartingFrom(req.getDepartingFrom());
        q.setTransport(req.getTransport());
        q.setStarRating(req.getStarRating());
        q.setPaxCount(req.getPaxCount());
        q.setProfitMargin(req.getProfitMargin());

        double totalCost = req.getCosts().stream()
                .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0).sum();
        double margin    = req.getProfitMargin() / 100.0;
        double ppp       = margin >= 1 ? 0 : Math.ceil((totalCost / (1 - margin)) / 50) * 50;
        q.setPricePerPerson(ppp);
        q.setTotalAmount(ppp * req.getPaxCount());

        q.getCosts().clear();
        int i = 0;
        for (QuoteCostRequest cr : req.getCosts()) {
            q.getCosts().add(QuoteCost.builder().quote(q)
                    .label(cr.getLabel()).amount(cr.getAmount())
                    .sortOrder(cr.getSortOrder() != null ? cr.getSortOrder() : i++).build());
        }

        q.getDays().clear();
        for (QuoteDayRequest dr : req.getDays()) {
            q.getDays().add(QuoteDay.builder().quote(q)
                    .dayNumber(dr.getDayNumber()).location(dr.getLocation())
                    .dateLabel(dr.getDateLabel()).hotel(dr.getHotel())
                    .sights(dr.getSights()).note(dr.getNote()).imageUrl(dr.getImageUrl()).build());
        }
    }

    /** Sinh quote number — dùng date range thay YEAR() để tương thích MySQL */
//    private String generateQuoteNumber(String userId) {
//        int year = Year.now().getValue();
//        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
//        LocalDateTime end   = LocalDateTime.of(year + 1, 1, 1, 0, 0);
//        long seq = quoteRepository.countByUserIdAndYearRange(userId, start, end) + 1;
//        return "TL-%d-%03d".formatted(year, seq);
//    }

    private String generateQuoteNumber(String userId) {
        int year = Year.now().getValue();
        String prefix = "TL-" + year + "-";
        long seq = quoteRepository.countByQuoteNumberPrefix(prefix) + 1;

        // Nếu bị trùng thì tăng tiếp
        String quoteNumber = "%s%03d".formatted(prefix, seq);
        while (quoteRepository.existsByQuoteNumber(quoteNumber)) {
            seq++;
            quoteNumber = "%s%03d".formatted(prefix, seq);
        }
        return quoteNumber;
    }



    public QuoteListItemResponse toListItem(Quote q) {
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

    public PageResponse<QuoteListItemResponse> getAllQuotes(int page, int size) {
        String userId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Quote> result = quoteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.<QuoteListItemResponse>builder()
                .content(result.getContent().stream().map(this::toListItem).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public PageResponse<QuoteListItemResponse> searchQuotes(String keyword, QuoteStatus status, int page, int size) {
        String userId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<Quote> result = quoteRepository.searchQuotes(userId, status, keyword, pageable);
        return PageResponse.<QuoteListItemResponse>builder()
                .content(result.getContent().stream().map(this::toListItem).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    public QuoteResponse toResponse(Quote q) {
        return QuoteResponse.builder()
                .id(q.getId()).quoteNumber(q.getQuoteNumber())
                .clientName(q.getClientName()).clientEmail(q.getClientEmail())
                .tourName(q.getTourName()).tourType(q.getTourType())
                .startDate(q.getStartDate()).endDate(q.getEndDate())
                .routeFrom(q.getRouteFrom()).routeTo(q.getRouteTo())
                .arrivingAt(q.getArrivingAt()).departingFrom(q.getDepartingFrom())
                .transport(q.getTransport()).starRating(q.getStarRating())
                .paxCount(q.getPaxCount()).profitMargin(q.getProfitMargin())
                .pricePerPerson(q.getPricePerPerson()).totalAmount(q.getTotalAmount())
                .status(q.getStatus())
                .costs(q.getCosts().stream().map(c -> QuoteCostResponse.builder()
                        .id(c.getId()).label(c.getLabel())
                        .amount(c.getAmount()).sortOrder(c.getSortOrder()).build()).toList())
                .days(q.getDays().stream().map(d -> QuoteDayResponse.builder()
                        .id(d.getId()).dayNumber(d.getDayNumber())
                        .location(d.getLocation()).dateLabel(d.getDateLabel())
                        .hotel(d.getHotel()).sights(d.getSights())
                        .note(d.getNote()).imageUrl(d.getImageUrl()).build()).toList())
                .createdAt(q.getCreatedAt() != null
                        ? q.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .updatedAt(q.getUpdatedAt() != null
                        ? q.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }
}
