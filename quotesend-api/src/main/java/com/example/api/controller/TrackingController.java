package com.example.api.controller;

import com.example.api.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    /**
     * GET /track/open/{token}
     * Called when the client's email client loads the tracking pixel.
     * Returns a 1×1 transparent GIF.
     */
    @GetMapping("/open/{token}")
    public ResponseEntity<byte[]> trackOpen(@PathVariable String token) {
        byte[] gif = trackingService.trackOpen(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/gif")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(gif);
    }

    /**
     * GET /track/click/{token}
     * Called when the client clicks a tracked link.
     * Records the click and redirects to company website.
     */
    @GetMapping("/click/{token}")
    public ResponseEntity<Void> trackClick(@PathVariable String token) {
        trackingService.trackClick(token);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("https://www.touristleader.com"))
                .build();
    }
}
