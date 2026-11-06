package com.lifestats.model;

/** 품목 분류. section은 통계 화면에서 묶여 보이는 구역 이름이다. */
public enum Category {
    ELEMENT("원소", "원소"),
    BYPRODUCT_RARE("부산물·레어", "부산물"),
    BYPRODUCT_EPIC("부산물·에픽", "부산물"),
    CRYSTAL("결정체", "부산물"),
    RELIC("유물", "유물"),
    COMMON("공통", "공통");

    public final String label;
    public final String section;

    Category(String label, String section) {
        this.label = label;
        this.section = section;
    }

    public static Category fromId(String id) {
        for (Category c : values()) {
            if (c.name().equalsIgnoreCase(id)) return c;
        }
        return null;
    }
}
