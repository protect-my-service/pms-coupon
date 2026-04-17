package com.pms.coupon.domain.member.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pms.coupon.common.response.CommonResponse;
import com.pms.coupon.common.response.PageInfo;
import com.pms.coupon.domain.coupon.dto.CouponIssueListResponse;
import com.pms.coupon.domain.member.dto.MemberCreateRequest;
import com.pms.coupon.domain.member.dto.MemberCreateResponse;
import com.pms.coupon.domain.member.dto.MemberListResponse;
import com.pms.coupon.domain.member.service.MemberService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/members")
@Validated
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public CommonResponse<List<MemberListResponse>> getMembers(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Page<MemberListResponse> result = memberService.getMembers(page, size);
        return CommonResponse.success(result.getContent(), PageInfo.from(result));
    }

    @GetMapping("/{id}/coupon-issues")
    @ResponseStatus(HttpStatus.OK)
    public CommonResponse<List<CouponIssueListResponse>> getMemberCouponIssues(
        @PathVariable("id") Long id,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Page<CouponIssueListResponse> result = memberService.getMemberCouponIssues(id, page, size);
        return CommonResponse.success(result.getContent(), PageInfo.from(result));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<MemberCreateResponse> create(@Valid @RequestBody MemberCreateRequest request) {
        return CommonResponse.success(memberService.create(request));
    }
}
