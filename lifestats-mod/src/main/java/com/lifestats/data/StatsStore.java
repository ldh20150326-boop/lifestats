package com.lifestats.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.lifestats.LifeStatsClient;
import com.lifestats.model.Period;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/** 날짜별 수집량과 생직 시간. 일/주/월 통계는 날짜별 기록을 합산해서 만든다. */
public final class StatsStore {
    public static final class Day {
        /** 직업 id -> (품목명 -> 개수) */
        public Map<String, Map<String, Long>> items = new HashMap<>();
        /** 직업 id -> 활동 초 */
        public Map<String, Long> seconds = new HashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, Day> days = new TreeMap<>();
    private static boolean dirty;

    private StatsStore() {
    }

    public static void load() {
        try {
            String s = ConfigFiles.readIfExists("stats.json");
            if (s == null) return;
            Map<String, Day> m = GSON.fromJson(s, new TypeToken<Map<String, Day>>() {
            }.getType());
            if (m != null) days = new TreeMap<>(m);
        } catch (Exception e) {
            LifeStatsClient.LOG.error("stats.json 을 읽지 못했습니다", e);
        }
    }

    public static void save() {
        if (!dirty) return;
        ConfigFiles.write("stats.json", GSON.toJson(days));
        dirty = false;
    }

    private static Day today() {
        return days.computeIfAbsent(LocalDate.now().toString(), k -> new Day());
    }

    public static void add(String jobId, String item, long n) {
        if (n <= 0) return;
        today().items.computeIfAbsent(jobId, k -> new HashMap<>()).merge(item, n, Long::sum);
        dirty = true;
    }

    public static void addSeconds(String jobId, long s) {
        today().seconds.merge(jobId, s, Long::sum);
        dirty = true;
    }

    public static Map<String, Long> totals(Period p, String jobId) {
        LocalDate now = LocalDate.now();
        Map<String, Long> out = new HashMap<>();
        for (Map.Entry<String, Day> e : days.entrySet()) {
            if (!inPeriod(p, e.getKey(), now)) continue;
            Map<String, Long> m = e.getValue().items.get(jobId);
            if (m != null) m.forEach((k, v) -> out.merge(k, v, Long::sum));
        }
        return out;
    }

    public static long seconds(Period p, String jobId) {
        LocalDate now = LocalDate.now();
        long total = 0;
        for (Map.Entry<String, Day> e : days.entrySet()) {
            if (!inPeriod(p, e.getKey(), now)) continue;
            total += e.getValue().seconds.getOrDefault(jobId, 0L);
        }
        return total;
    }

    private static boolean inPeriod(Period p, String dateKey, LocalDate now) {
        try {
            return p.contains(LocalDate.parse(dateKey), now);
        } catch (Exception e) {
            return false;
        }
    }
}
