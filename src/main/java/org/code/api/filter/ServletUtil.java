package org.code.api.filter;

import java.io.IOException;

import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletResponse;

public class ServletUtil {
    public static void sendRefusedResponse(
        HttpServletResponse response,
        HttpStatus status,
        String error_code,
        String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setHeader("Content-Type", "application/json");
        response.getWriter().write(String.format("{\"error\":\"%s\",\"message\":\"%s\"}", error_code, message));
    }
}
