package com.lifestats.model;

/** 생활 직업. COMMON은 직업 공통 품목(생활의 교본 등)을 담는 내부용 항목이라 탭에는 나오지 않는다. */
public enum Job {
    MINER("광부", true),
    FARMER("농부", true),
    FISHER("어부", true),
    WOODCUTTER("나무꾼", true),
    HUNTER("사냥꾼", true),
    COMMON("공통", false);

    public final String label;
    public final boolean tab;

    Job(String label, boolean tab) {
        this.label = label;
        this.tab = tab;
    }

    public static Job fromId(String id) {
        for (Job j : values()) {
            if (j.name().equalsIgnoreCase(id)) return j;
        }
        return null;
    }

    public static Job[] tabs() {
        return new Job[]{MINER, FARMER, FISHER, WOODCUTTER, HUNTER};
    }
}
