package com.pms.coupon.common.response;

import com.pms.coupon.common.exception.ResponseCode;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommonResponse<T>(
    String message,
    T data,
    PageInfo pageInfo
) {

    private static final String SUCCESS_MESSAGE = "요청이 성공했습니다.";

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(SUCCESS_MESSAGE, data, null);
    }

    public static <T> CommonResponse<T> success(T data, PageInfo pageInfo) {
        return new CommonResponse<>(SUCCESS_MESSAGE, data, pageInfo);
    }

    public static CommonResponse<Void> error(ResponseCode responseCode) {
        return new CommonResponse<>(responseCode.getMessage(), null, null);
    }
}
