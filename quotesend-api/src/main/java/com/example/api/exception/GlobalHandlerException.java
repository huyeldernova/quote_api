package com.example.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import java.util.List;

@RestControllerAdvice @Slf4j
public class GlobalHandlerException {

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest req) {
        log.error("Unhandled exception", ex);
        ErrorCode ec = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(ec.getHttpStatus()).body(build(ec, req));
    }

    @ExceptionHandler(AppException.class)
    ResponseEntity<ErrorResponse> handleAppException(AppException ex, WebRequest req) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity.status(ec.getHttpStatus()).body(build(ec, req));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList();
        String msg = errors.isEmpty() ? "Validation failed" : String.join(", ", errors);
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .code(400).status(400).message(msg).error("Bad Request")
                .path(path(req)).build());
    }

    private ErrorResponse build(ErrorCode ec, WebRequest req) {
        return ErrorResponse.builder()
                .code(ec.getCode()).status(ec.getHttpStatus().value())
                .message(ec.getMessage())
                .error(ec.getHttpStatus().getReasonPhrase())
                .path(path(req)).build();
    }
    private String path(WebRequest req) {
        return req.getDescription(false).replace("uri=", "");
    }
}
