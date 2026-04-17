package com.pms.coupon.common.exception;

import com.pms.coupon.common.response.CommonResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessCustomException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusinessException(BusinessCustomException e) {
        ResponseCode responseCode = e.getResponseCode();
        return ResponseEntity.status(responseCode.getHttpStatus())
            .body(CommonResponse.error(responseCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? ResponseCode.INVALID_REQUEST.getMessage() : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest()
            .body(new CommonResponse<>(message, null, null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
            .findFirst()
            .map(violation -> violation.getMessage())
            .orElse(ResponseCode.INVALID_REQUEST.getMessage());
        return ResponseEntity.badRequest()
            .body(new CommonResponse<>(message, null, null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
            .body(CommonResponse.error(ResponseCode.INVALID_REQUEST));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<CommonResponse<Void>> handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String message = e.getParameterValidationResults().stream()
            .flatMap((ParameterValidationResult result) -> result.getResolvableErrors().stream())
            .findFirst()
            .map(error -> error.getDefaultMessage())
            .orElse(ResponseCode.INVALID_REQUEST.getMessage());
        return ResponseEntity.badRequest()
            .body(new CommonResponse<>(message, null, null));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<CommonResponse<Void>> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        return ResponseEntity.badRequest()
            .body(CommonResponse.error(ResponseCode.INVALID_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleUnknownException(Exception e) {
        ResponseCode responseCode = ResponseCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(responseCode.getHttpStatus())
            .body(CommonResponse.error(responseCode));
    }
}
