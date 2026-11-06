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

public final class ItemRegistry {
    private static final Map<String, ItemDef> BY_NAME = new HashMap<>();
    private static final Map<Job, List<ItemDef>> BY_JOB = new EnumMap<>(Job.class);

    private ItemRegistry() {
    }

    public static void load() {
        BY_NAME.clear();
        BY_JOB.clear();
        for (Job j : Job.values()) BY_JOB.put(j, new ArrayList<>());

        // [코드에 5대 직업 모든 품목 100% 내장]
        loadDefaults();

        try {
            String s = ConfigFiles.readIfExists("jobs.json");
            if (s != null && !s.isBlank()) {
                JsonObject root = JsonParser.parseString(s).getAsJsonObject();
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
            }
        } catch (Exception ex) {
            LifeStatsClient.LOG.warn("외부 설정 없음, 내장 데이터로 실행");
        }
    }

    private static void loadDefaults() {
        // [공통]
        add(new ItemDef("생활의 교본", Job.COMMON, Category.COMMON));

        // [광부]
        add(new ItemDef("땅의 원소 가루", Job.MINER, Category.ELEMENT));
        add(new ItemDef("땅의 원소 조각", Job.MINER, Category.ELEMENT));
        add(new ItemDef("땅의 원소 정수", Job.MINER, Category.ELEMENT));

        add(new ItemDef("솔라이트 원석", Job.MINER, Category.BYPRODUCT_RARE));
        add(new ItemDef("가넷", Job.MINER, Category.BYPRODUCT_RARE));
        add(new ItemDef("토파즈", Job.MINER, Category.BYPRODUCT_RARE));
        add(new ItemDef("페리도트", Job.MINER, Category.BYPRODUCT_RARE));
        add(new ItemDef("아쿠아마린", Job.MINER, Category.BYPRODUCT_RARE));

        add(new ItemDef("암모나이트 화석", Job.MINER, Category.RELIC));
        add(new ItemDef("거미 화석", Job.MINER, Category.REL
