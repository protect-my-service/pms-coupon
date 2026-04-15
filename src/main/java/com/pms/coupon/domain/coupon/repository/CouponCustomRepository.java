package com.pms.coupon.domain.coupon.repository;

import com.pms.coupon.domain.coupon.entity.Coupon;
import java.util.Optional;

public interface CouponCustomRepository {

    Optional<Coupon> findByIdWithPessimisticLock(Long couponId);
}
