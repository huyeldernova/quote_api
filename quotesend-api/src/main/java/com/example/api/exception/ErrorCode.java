package com.example.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(500, "Internal server error",           HttpStatus.INTERNAL_SERVER_ERROR),
    ACCESS_DENIED        (403, "Access denied",                   HttpStatus.FORBIDDEN),
    TOKEN_GENERATION_FAILED(500,"Failed to generate JWT token",   HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_EXPIRED        (401, "Token expired",                   HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID        (401, "Token invalid",                   HttpStatus.UNAUTHORIZED),
    USER_EXISTED         (400, "User already exists",             HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND       (404, "User not found",                  HttpStatus.NOT_FOUND),
    QUOTE_NOT_FOUND      (404, "Quote not found",                 HttpStatus.NOT_FOUND),
    QUOTE_ACCESS_DENIED  (403, "You do not own this quote",       HttpStatus.FORBIDDEN),
    EMAIL_LOG_NOT_FOUND  (404, "Email log not found",             HttpStatus.NOT_FOUND),
    FILE_EMPTY           (400, "File is empty",                   HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE       (400, "File exceeds maximum allowed size",HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE    (400, "Only image files are allowed",    HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED   (500, "Failed to upload file",           HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_SEND_FAILED    (500, "Failed to send email",            HttpStatus.INTERNAL_SERVER_ERROR);

    private final int        code;
    private final String     message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code       = code;
        this.message    = message;
        this.httpStatus = httpStatus;
    }
}
