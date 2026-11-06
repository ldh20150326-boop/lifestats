package com.lifestats.model;

/** 추적 대상 품목 하나. name은 게임 내 아이템 표시 이름(서식 제거 후)과 정확히 같아야 한다. */
public record ItemDef(String name, Job job, Category category) {
}
