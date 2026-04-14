package com.pms.coupon.common.response;

import com.pms.coupon.common.exception.ResponseCode;

public record CommonResponse<T>(
    String message,
    T data
) {

    public static <T> CommonResponse<T> success(ResponseCode responseCode, T data) {
        return new CommonResponse<>(responseCode.getMessage(), data);
    }

    public static CommonResponse<Void> error(ResponseCode responseCode) {
        return new CommonResponse<>(responseCode.getMessage(), null);
    }
}
