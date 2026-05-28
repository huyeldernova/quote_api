package com.example.api.controller;

import com.example.api.dto.response.ImageUploadResponse;
import com.example.api.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /** POST /api/v1/images/upload  (multipart/form-data, field = "file") */
    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> upload(
            @RequestParam("file") MultipartFile file) {
        String url = imageService.upload(file);
        return ResponseEntity.ok(ImageUploadResponse.builder().imageUrl(url).build());
    }

    /** DELETE /api/v1/images?url=... */
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam("url") String imageUrl) {
        imageService.delete(imageUrl);
        return ResponseEntity.noContent().build();
    }
}
