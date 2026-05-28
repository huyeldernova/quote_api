package com.example.api.controller;

import com.example.api.dto.request.QuoteFormRequest;
import com.example.api.dto.request.SendEmailRequest;
import com.example.api.dto.response.*;
import com.example.api.enums.QuoteStatus;
import com.example.api.service.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;


    /** POST /api/v1/quotes */
    @PostMapping
    public ResponseEntity<QuoteResponse> create(@Valid @RequestBody QuoteFormRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.createQuote(req));
    }

    /** GET /api/v1/quotes/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.getQuote(id));
    }

    /** PUT /api/v1/quotes/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<QuoteResponse> update(
            @PathVariable Long id, @Valid @RequestBody QuoteFormRequest req) {
        return ResponseEntity.ok(quoteService.updateQuote(id, req));
    }

    /** DELETE /api/v1/quotes/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quoteService.deleteQuote(id);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/v1/quotes/{id}/duplicate */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<DuplicateQuoteResponse> duplicate(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.duplicateQuote(id));
    }

    /** GET /api/v1/quotes/{id}/pdf  →  returns PDF blob */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        byte[] pdf = quoteService.generatePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"quote.pdf\"")
                .body(pdf);
    }

    /** POST /api/v1/quotes/{id}/send */
    @PostMapping("/{id}/send")
    public ResponseEntity<SendEmailResponse> sendEmail(
            @PathVariable Long id, @Valid @RequestBody SendEmailRequest req) {
        return ResponseEntity.ok(quoteService.sendEmail(id, req));
    }

    /** GET /api/v1/quotes?page=0&size=10 */
    @GetMapping
    public ResponseEntity<PageResponse<QuoteListItemResponse>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(quoteService.getAllQuotes(page, size));
    }

    /** GET /api/v1/quotes/search?keyword=vietnam&status=DRAFT&page=0&size=10 */
    @GetMapping("/search")
    public ResponseEntity<PageResponse<QuoteListItemResponse>> search(
            @RequestParam(required = false)    String keyword,
            @RequestParam(required = false)    QuoteStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(quoteService.searchQuotes(keyword, status, page, size));
    }

    /** GET /api/v1/quotes/{id}/preview — xem PDF inline trên browser */
    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> previewPdf(@PathVariable Long id) {
        byte[] pdf = quoteService.generatePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"quote.pdf\"")
                .body(pdf);
    }
}
