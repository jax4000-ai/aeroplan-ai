package com.aeroplan.scheduling.common;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/** Single place where exceptions become RFC 9457 Problem Details responses. */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ProblemDetail apiException(ApiException exception, HttpServletRequest request) {
        ErrorCode code = exception.code();
        log.warn("errorCode={} status={} path={} reason=\"{}\"", code, code.status().value(), request.getRequestURI(), exception.getMessage());
        return problem(code.status(), code.title(), exception.getMessage(), code);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationFailed(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("The request failed validation");
        log.warn("errorCode={} status=400 path={} reason=\"{}\"", ErrorCode.REQUEST_VALIDATION_FAILED, request.getRequestURI(), detail);
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.REQUEST_VALIDATION_FAILED.title(), detail, ErrorCode.REQUEST_VALIDATION_FAILED);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        log.error("errorCode={} status=500 path={} unhandled exception", ErrorCode.INTERNAL_ERROR, request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR.title(), "An unexpected error occurred", ErrorCode.INTERNAL_ERROR);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, ErrorCode code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://aeroplan.example/problems/" + status.value()));
        problem.setProperty("errorCode", code.name());
        return problem;
    }
}
