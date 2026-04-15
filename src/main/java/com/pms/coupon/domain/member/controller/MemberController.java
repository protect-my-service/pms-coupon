package com.pms.coupon.domain.member.controller;

import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.common.response.CommonResponse;
import com.pms.coupon.domain.member.dto.MemberCreateRequest;
import com.pms.coupon.domain.member.dto.MemberCreateResponse;
import com.pms.coupon.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members")
@Validated
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<MemberCreateResponse> create(@Valid @RequestBody MemberCreateRequest request) {
        MemberCreateResponse result = memberService.create(request);
        return CommonResponse.success(ResponseCode.MEMBER_CREATE_SUCCESS, result);
    }
}
