package com.lifestats.track;

import com.lifestats.data.StatsStore;
import com.lifestats.model.Job;

import java.util.EnumMap;
import java.util.Map;

/** 생직 시간: 직업 품목을 마지막으로 얻은 뒤 IDLE_MS 안이면 그 직업의 활동 시간으로 센다. */
public final class PlayTimer {
    private static final long IDLE_MS = 60_000L;
    private static final Map<Job, Long> LAST_GAIN = new EnumMap<>(Job.class);
    private static int tick;

    private PlayTimer() {
    }

    public static void noteGain(Job job) {
        LAST_GAIN.put(job, System.currentTimeMillis());
    }

    public static void onTick() {
        if (++tick < 20) return;
        tick = 0;
        long now = System.currentTimeMillis();
        for (Map.Entry<Job, Long> e : LAST_GAIN.entrySet()) {
            if (now - e.getValue() <= IDLE_MS) {
                StatsStore.addSeconds(e.getKey().name(), 1);
            }
        }
    }
}
