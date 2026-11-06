package com.lifestats.buff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.lifestats.LifeStatsClient;
import com.lifestats.data.ConfigFiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 버프 효과 시간과 재사용 대기 타이머.
 * 발동 감지는 (1) 해당 아이템을 사용했을 때 이름 매칭, (2) 채팅/액션바 메시지 정규식 매칭 두 가지다.
 * 남은 시간은 종료 시각(epoch ms)으로 저장해서 게임을 껐다 켜도 이어진다.
 */
public final class BuffManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<BuffDef> DEFS = new ArrayList<>();
    /** id -> {효과 종료 시각, 재사용 종료 시각} */
    private static Map<String, long[]> state = new HashMap<>();
    private static boolean dirty;

    private BuffManager() {
    }

    public static void load() {
        DEFS.clear();
        try {
            List<BuffDef> list = GSON.fromJson(ConfigFiles.readWithDefault("buffs.json"),
                    new TypeToken<List<BuffDef>>() {
                    }.getType());
            if (list != null) {
                for (BuffDef d : list) {
                    if (d == null || d.id == null || d.name == null) continue;
                    d.compile();
                    DEFS.add(d);
                }
            }
            String s = ConfigFiles.readIfExists("buff_state.json");
            if (s != null) {
                Map<String, long[]> m = GSON.fromJson(s, new TypeToken<Map<String, long[]>>() {
                }.getType());
                if (m != null) state = new HashMap<>(m);
            }
        } catch (Exception e) {
            LifeStatsClient.LOG.error("buffs.json 을 읽지 못했습니다", e);
        }
    }

    public static void saveState() {
        if (!dirty) return;
        ConfigFiles.write("buff_state.json", GSON.toJson(state));
        dirty = false;
    }

    public static List<BuffDef> defs() {
        return DEFS;
    }

    public static long effectLeftSec(BuffDef d) {
        long[] s = state.get(d.id);
        return s == null ? 0 : leftSec(s[0]);
    }

    public static long cooldownLeftSec(BuffDef d) {
        long[] s = state.get(d.id);
        return s == null ? 0 : leftSec(s[1]);
    }

    private static long leftSec(long endMs) {
        long left = endMs - System.currentTimeMillis();
        return left <= 0 ? 0 : (left + 999) / 1000;
    }

    public static void onItemUsed(String itemName) {
        if (itemName == null || itemName.isEmpty()) return;
        for (BuffDef d : DEFS) {
            if (d.match != null && !d.match.isEmpty() && itemName.contains(d.match)) start(d);
        }
    }

    public static void onMessage(String message) {
        if (message == null || message.isEmpty()) return;
        for (BuffDef d : DEFS) {
            if (d.chatPattern != null && d.chatPattern.matcher(message).find()) start(d);
        }
    }

    private static void start(BuffDef d) {
        long now = System.currentTimeMillis();
        long[] cur = state.get(d.id);
        // 재사용 대기 중에 다시 눌러도 타이머가 초기화되지 않게 한다.
        if (cur != null && cur[1] > now) return;
        long effectEnd = d.effectSeconds > 0 ? now + d.effectSeconds * 1000L : 0;
        long cooldownEnd = d.cooldownSeconds > 0 ? now + d.cooldownSeconds * 1000L : 0;
        state.put(d.id, new long[]{effectEnd, cooldownEnd});
        dirty = true;
    }
}
