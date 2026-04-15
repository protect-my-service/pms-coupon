package com.pms.coupon.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class DateUtils {

    public static LocalDateTime toStartOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    public static LocalDateTime toEndOfDay(LocalDate date) {
        return date.atTime(LocalTime.of(23, 59, 59));
    }

    public static boolean isValidDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return !startDate.isAfter(endDate);
    }
}
