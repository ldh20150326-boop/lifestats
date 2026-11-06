package com.lifestats.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lifestats.LifeStatsClient;
import com.lifestats.model.Category;
import com.lifestats.model.ItemDef;
import com.lifestats.model.Job;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** config/lifestats/jobs.json 에서 직업별 추적 품목을 읽는다. 품목을 늘리려면 이 파일만 고치면 된다. */
public final class ItemRegistry {
    private static final Map<String, ItemDef> BY_NAME = new HashMap<>();
    private static final Map<Job, List<ItemDef>> BY_JOB = new EnumMap<>(Job.class);

    private ItemRegistry() {
    }

    public static void load() {
        BY_NAME.clear();
        BY_JOB.clear();
        for (Job j : Job.values()) BY_JOB.put(j, new ArrayList<>());
        try {
            JsonObject root = JsonParser.parseString(ConfigFiles.readWithDefault("jobs.json")).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                Job job = Job.fromId(e.getKey());
                if (job == null) continue;
                if (job == Job.COMMON) {
                    for (JsonElement n : e.getValue().getAsJsonArray()) {
                        add(new ItemDef(n.getAsString(), Job.COMMON, Category.COMMON));
                    }
                } else {
                    for (Map.Entry<String, JsonElement> c : e.getValue().getAsJsonObject().entrySet()) {
                        Category cat = Category.fromId(c.getKey());
                        if (cat == null) continue;
                        for (JsonElement n : c.getValue().getAsJsonArray()) {
                            add(new ItemDef(n.getAsString(), job, cat));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LifeStatsClient.LOG.error("jobs.json 을 읽지 못했습니다", ex);
        }
    }

    private static void add(ItemDef def) {
        BY_NAME.putIfAbsent(def.name(), def);
        BY_JOB.get(def.job()).add(def);
    }

    public static ItemDef byName(String name) {
        return BY_NAME.get(name);
    }

    public static List<ItemDef> items(Job job) {
        return BY_JOB.getOrDefault(job, List.of());
    }
}
