package com.pms.coupon.common.exception;

import lombok.Getter;

@Getter
public class BusinessCustomException extends RuntimeException {

    private final ResponseCode responseCode;

    public BusinessCustomException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }
}
