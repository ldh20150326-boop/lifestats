package com.lifestats.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

public enum Period {
    DAY("일간"),
    WEEK("주간"),
    MONTH("월간");

    public final String label;

    Period(String label) {
        this.label = label;
    }

    /** d가 today 기준으로 이 기간 안에 들어가는지. 주간은 월요일 시작. */
    public boolean contains(LocalDate d, LocalDate today) {
        if (d.isAfter(today)) return false;
        return switch (this) {
            case DAY -> d.equals(today);
            case WEEK -> !d.isBefore(today.with(DayOfWeek.MONDAY));
            case MONTH -> d.getYear() == today.getYear() && d.getMonth() == today.getMonth();
        };
    }
}
