package com.pms.coupon.domain.member.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pms.coupon.common.exception.BusinessCustomException;
import com.pms.coupon.common.exception.ResponseCode;
import com.pms.coupon.domain.coupon.dto.CouponIssueListResponse;
import com.pms.coupon.domain.coupon.entity.CouponIssue;
import com.pms.coupon.domain.coupon.repository.CouponIssueRepository;
import com.pms.coupon.domain.member.dto.MemberCreateRequest;
import com.pms.coupon.domain.member.dto.MemberCreateResponse;
import com.pms.coupon.domain.member.dto.MemberListResponse;
import com.pms.coupon.domain.member.entity.Member;
import com.pms.coupon.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final CouponIssueRepository couponIssueRepository;

    /**
     * 회원 등록
     * @param request
     */
    @Transactional
    public MemberCreateResponse create(MemberCreateRequest request) {
        Member member = memberRepository.save(Member.create(request.name().trim()));
        return new MemberCreateResponse(member.getId(), member.getName(), member.getCreatedAt());
    }

    /**
     * 회원 목록 조회
     * @param page
     * @param size
     */
    @Transactional(readOnly = true)
    public Page<MemberListResponse> getMembers(int page, int size) {
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            )
        );
        Page<Member> resultPage = memberRepository.findAll(pageable);

        return resultPage.map(member -> MemberListResponse.builder()
            .memberId(member.getId())
            .name(member.getName())
            .createdAt(member.getCreatedAt())
            .build());
    }

    /**
     * 특정 회원의 쿠폰 발급 이력 조회
     * @param memberId
     * @param page
     * @param size
     */
    @Transactional(readOnly = true)
    public Page<CouponIssueListResponse> getMemberCouponIssues(Long memberId, int page, int size) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessCustomException(ResponseCode.MEMBER_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            )
        );
        Page<CouponIssue> resultPage = couponIssueRepository.findAllByMemberId(memberId, pageable);

        return resultPage.map(couponIssue -> CouponIssueListResponse.builder()
            .couponIssueId(couponIssue.getId())
            .couponId(couponIssue.getCouponId())
            .memberId(couponIssue.getMemberId())
            .issuedDate(couponIssue.getIssuedDate())
            .build());
    }
}
