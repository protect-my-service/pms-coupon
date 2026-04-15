package com.pms.coupon.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    COUPON_CREATE_SUCCESS(HttpStatus.CREATED, "쿠폰이 생성되었습니다."),
    COUPON_ISSUE_SUCCESS(HttpStatus.OK, "쿠폰이 발급되었습니다."),
    COUPON_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "쿠폰 이름은 필수입니다."),
    COUPON_TOTAL_QUANTITY_INVALID(HttpStatus.BAD_REQUEST, "쿠폰 발급 수량은 1 이상이어야 합니다."),
    COUPON_ISSUE_PERIOD_INVALID_ON_CREATE(HttpStatus.BAD_REQUEST, "쿠폰 발급 시작일은 종료일보다 빨라야 합니다."),
    MEMBER_CREATE_SUCCESS(HttpStatus.CREATED, "회원이 등록되었습니다."),
    MEMBER_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "회원 이름은 필수입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급된 쿠폰입니다."),
    COUPON_ISSUE_REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "쿠폰 발급 요청이 처리 중입니다."),
    COUPON_SOLD_OUT(HttpStatus.CONFLICT, "쿠폰이 모두 소진되었습니다."),
    COUPON_ISSUE_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "쿠폰 발급 가능 기간이 아닙니다."),
    COUPON_REDIS_KEY_NOT_INITIALIZED(HttpStatus.INTERNAL_SERVER_ERROR, "쿠폰 Redis 키가 초기화되지 않았습니다."),
    COUPON_REDIS_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "쿠폰 Redis 처리 중 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
