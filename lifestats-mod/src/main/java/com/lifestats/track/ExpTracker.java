package com.lifestats.track;

import net.minecraft.client.MinecraftClient;

/**
 * 경험치 시작/현재 표시용. 기본은 바닐라 경험치 바(레벨 + 진행률)를 읽는다.
 * 서버가 생활 레벨을 다른 곳(스코어보드, 보스바 등)으로 알려준다면 current* 를 그쪽에서 읽도록 바꾸면 된다.
 */
public final class ExpTracker {
    private static int startLevel = -1;
    private static float startProgress;

    private ExpTracker() {
    }

    public static void reset(MinecraftClient mc) {
        if (mc.player == null) return;
        startLevel = mc.player.experienceLevel;
        startProgress = mc.player.experienceProgress;
    }

    public static void ensureStarted(MinecraftClient mc) {
        if (startLevel < 0) reset(mc);
    }

    public static boolean ready() {
        return startLevel >= 0;
    }

    public static int startLevel() {
        return startLevel;
    }

    public static float startProgress() {
        return startProgress;
    }

    public static int currentLevel(MinecraftClient mc) {
        return mc.player == null ? 0 : mc.player.experienceLevel;
    }

    public static float currentProgress(MinecraftClient mc) {
        return mc.player == null ? 0f : mc.player.experienceProgress;
    }
}
