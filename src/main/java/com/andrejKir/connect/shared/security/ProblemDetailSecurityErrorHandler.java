package com.andrejKir.connect.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProblemDetailSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final MessageSource messageSource;
    private final JsonMapper jsonMapper;

    public ProblemDetailSecurityErrorHandler(MessageSource messageSource, JsonMapper jsonMapper) {
        this.messageSource = messageSource;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "error.authentication.required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String messageKey = accessDeniedException instanceof CsrfException
                ? "error.csrf.token"
                : "error.access.denied";
        write(request, response, HttpStatus.FORBIDDEN, messageKey);
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, String messageKey) throws IOException {
        String message = messageSource.getMessage(messageKey, null, request.getLocale());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getWriter(), problemDetail);
    }
}