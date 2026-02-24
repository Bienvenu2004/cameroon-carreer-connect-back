package com.hostdesign24.jobportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PasswordNotMatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordNotMatchException(
            PasswordNotMatchException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("PASSWORD_NOT_MATCH");
        errorResponse.setStatus(400);
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(DeviceBlockedException.class)
    public ResponseEntity<ErrorResponse> deviceBlockedException(
            DeviceBlockedException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("USER_DEVICE_BLOCKED");
        errorResponse.setStatus(HttpStatus.FORBIDDEN.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(
            UserAlreadyExistsException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("USER_ALREADY_EXISTS");
        errorResponse.setStatus(400);
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(java.lang.SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurityException(SecurityException e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "AUTHENTICATION_FAILED");
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        var error = new HashMap<String, String>();
        e.getBindingResult()
                .getFieldErrors()
                .forEach(
                        fieldError -> error.put(fieldError.getField(), fieldError.getDefaultMessage()));

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("NOT_FOUND");
        errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUploadException(FileUploadException e,
                                                                   WebRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        error.setErrorCode("FILE_UPLOAD_ERROR");
        error.setStatus(400);
        error.setTimestamp(System.currentTimeMillis());
        error.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e,
                                                                     WebRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        error.setErrorCode("FILE_UPLOAD_SIZE_EXCEEDED");
        error.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        error.setTimestamp(System.currentTimeMillis());
        error.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleExceptionNoResourceFoundException(
            NoResourceFoundException e, WebRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        error.setErrorCode("NO_RESOURCE_FOUND");
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setTimestamp(System.currentTimeMillis());
        error.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DoesNotExistsException.class)
    public ResponseEntity<ErrorResponse> handleDoesNotExistsException(DoesNotExistsException e,
                                                                      WebRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        error.setErrorCode("DOES_NOT_EXIST");
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setTimestamp(System.currentTimeMillis());
        error.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e,
                                                                        WebRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        error.setErrorCode("ILLEGAL_ARGUMENT");
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setTimestamp(System.currentTimeMillis());
        error.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalStateException e,
                                                                        WebRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        error.setErrorCode("ILLEGAL_STATE");
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setTimestamp(System.currentTimeMillis());
        error.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataConflictException.class)
    public ResponseEntity<ErrorResponse> handleDataConflict(DataConflictException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setStatus(HttpStatus.CONFLICT.value());
        error.setErrorCode("DATA_CONFLICT");
        error.setMessage(ex.getMessage());
        error.setTimestamp(System.currentTimeMillis());
        error.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJwtException(InvalidJwtException e,
                                                                   WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("INVALID_TOKEN");
        errorResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequestsException(TooManyRequestsException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("TOO_MANY_REQUESTS");
        errorResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateRequestException(DuplicateRequestException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("DUPLICATE_REQUEST");
        errorResponse.setStatus(HttpStatus.CONFLICT.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(ActionDeniedException.class)
    public ResponseEntity<ErrorResponse> handleActionDeniedException(ActionDeniedException e,
                                                                     WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("ACTION_DENIED");
        errorResponse.setStatus(HttpStatus.FORBIDDEN.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatchException(
            PasswordMismatchException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("PASSWORD_MISMATCH");
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(
            InvalidTokenException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("INVALID_TOKEN");
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(
            TokenExpiredException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("TOKEN_EXPIRED");
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyAttemptsException(
            TooManyAttemptsException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("TOO_MANY_ATTEMPTS");
        errorResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(InvalidVerificationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationException(
            InvalidVerificationException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("INVALID_VERIFICATION");
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException e, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        errorResponse.setErrorCode("RATE_LIMIT_EXCEEDED");
        errorResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(DeviceIdGenerationException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(DeviceIdGenerationException e,
                                                                        WebRequest request) {
        ErrorResponse error = new ErrorResponse();
        error.setMessage(HtmlUtils.htmlEscape(e.getMessage()));
        error.setErrorCode("DEVICE_ID_GENERATION_FAILED");
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setTimestamp(System.currentTimeMillis());
        error.setErrorDescription(HtmlUtils.htmlEscape(request.getDescription(false)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
