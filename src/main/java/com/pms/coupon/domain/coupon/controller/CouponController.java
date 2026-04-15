package com.pms.coupon.domain.coupon.controller;

import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.common.response.CommonResponse;
import com.pms.coupon.domain.coupon.dto.CouponCreateRequest;
import com.pms.coupon.domain.coupon.dto.CouponCreateResponse;
import com.pms.coupon.domain.coupon.dto.CouponIssueResponse;
import com.pms.coupon.domain.coupon.service.CouponService;
import com.pms.coupon.domain.coupon.service.CouponIssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupons")
@Validated
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponIssueService couponIssueService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<CouponCreateResponse> create(
        @RequestHeader("X-Member-Id") Long memberId,
        @Valid @RequestBody CouponCreateRequest request
    ) {
        CouponCreateResponse result = couponService.create(memberId, request);
        return CommonResponse.success(ResponseCode.COUPON_CREATE_SUCCESS, result);
    }

    @PostMapping("/{id}/issue")
    public CommonResponse<CouponIssueResponse> issue(
        @RequestHeader("X-Member-Id") Long memberId,
        @PathVariable long id
    ) {
        CouponIssueResponse result = couponIssueService.issue(id, memberId);
        return CommonResponse.success(ResponseCode.COUPON_ISSUE_SUCCESS, result);
    }
}
