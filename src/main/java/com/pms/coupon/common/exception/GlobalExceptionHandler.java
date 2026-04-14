package com.pms.coupon.common.exception;

import com.pms.coupon.common.response.CommonResponse;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleUnknownException(Exception e) {
        ResponseCode responseCode = ResponseCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(responseCode.getHttpStatus())
            .body(CommonResponse.error(responseCode));
    }
}
