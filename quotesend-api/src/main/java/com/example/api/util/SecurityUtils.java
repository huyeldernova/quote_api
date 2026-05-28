package com.example.api.util;

import com.example.api.exception.AppException;
import com.example.api.exception.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {
    /** Returns the user UUID stored as JWT subject. */
    public String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return auth.getName(); // JWT subject = user UUID
    }
}
