package com.example.api.controller;

import com.example.api.dto.response.EmailLogResponse;
import com.example.api.dto.response.EmailStatsResponse;
import com.example.api.dto.response.PageResponse;
import com.example.api.service.EmailLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailLogService emailLogService;

    /** GET /api/v1/emails?page=0&size=10 */
    @GetMapping
    public ResponseEntity<PageResponse<EmailLogResponse>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(emailLogService.getAllLogs(page, size));
    }

    /** GET /api/v1/emails/stats */
    @GetMapping("/stats")
    public ResponseEntity<EmailStatsResponse> getStats() {
        return ResponseEntity.ok(emailLogService.getStats());
    }

    /** GET /api/v1/emails/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<EmailLogResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(emailLogService.getById(id));
    }
}
