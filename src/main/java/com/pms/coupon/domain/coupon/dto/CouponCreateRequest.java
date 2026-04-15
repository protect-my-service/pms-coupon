package com.pms.coupon.domain.coupon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CouponCreateRequest(
    @NotBlank(message = "쿠폰 이름은 필수입니다.")
    String name,
    @NotNull(message = "쿠폰 발급 수량은 필수입니다.")
    @Min(value = 1, message = "쿠폰 발급 수량은 1 이상이어야 합니다.")
    Integer totalQuantity,
    @NotNull(message = "쿠폰 발급 시작일은 필수입니다.")
    LocalDate issueStartDate,
    @NotNull(message = "쿠폰 발급 종료일은 필수입니다.")
    LocalDate issueEndDate
) {
}
