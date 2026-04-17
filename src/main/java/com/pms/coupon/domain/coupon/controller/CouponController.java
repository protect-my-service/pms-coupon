package com.pms.coupon.domain.coupon.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pms.coupon.common.response.CommonResponse;
import com.pms.coupon.common.response.PageInfo;
import com.pms.coupon.domain.coupon.dto.CouponCreateRequest;
import com.pms.coupon.domain.coupon.dto.CouponCreateResponse;
import com.pms.coupon.domain.coupon.dto.CouponIssueListResponse;
import com.pms.coupon.domain.coupon.dto.CouponIssueResponse;
import com.pms.coupon.domain.coupon.dto.CouponListResponse;
import com.pms.coupon.domain.coupon.service.CouponIssueService;
import com.pms.coupon.domain.coupon.service.CouponService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/coupons")
@Validated
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponIssueService couponIssueService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public CommonResponse<List<CouponListResponse>> getCoupons(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Page<CouponListResponse> result = couponService.getCoupons(page, size);
        return CommonResponse.success(result.getContent(), PageInfo.from(result));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<CouponCreateResponse> create(
        @RequestHeader("X-Member-Id") Long memberId,
        @Valid @RequestBody CouponCreateRequest request
    ) {
        return CommonResponse.success(couponService.create(memberId, request));
    }

    @PostMapping("/{id}/issue")
    @ResponseStatus(HttpStatus.OK)
    public CommonResponse<CouponIssueResponse> issue(
        @RequestHeader("X-Member-Id") Long memberId,
        @PathVariable long id
    ) {
        return CommonResponse.success(couponIssueService.issue(id, memberId));
    }

    @GetMapping("/issues")
    @ResponseStatus(HttpStatus.OK)
    public CommonResponse<List<CouponIssueListResponse>> getCouponIssues(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Page<CouponIssueListResponse> result = couponIssueService.getCouponIssues(page, size);
        return CommonResponse.success(result.getContent(), PageInfo.from(result));
    }
}
