package com.pms.coupon.domain.member.repository;

import com.pms.coupon.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
