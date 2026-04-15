package com.pms.coupon.domain.member.service;

import com.pms.coupon.common.exception.BusinessCustomException;
import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.domain.member.dto.MemberCreateRequest;
import com.pms.coupon.domain.member.dto.MemberCreateResponse;
import com.pms.coupon.domain.member.entity.Member;
import com.pms.coupon.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public MemberCreateResponse create(MemberCreateRequest request) {
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new BusinessCustomException(ResponseCode.MEMBER_NAME_REQUIRED);
        }

        Member member = memberRepository.save(Member.create(request.name().trim()));
        return new MemberCreateResponse(member.getId(), member.getName(), member.getCreatedAt());
    }
}
