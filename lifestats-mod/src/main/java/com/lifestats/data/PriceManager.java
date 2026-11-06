package com.lifestats.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.lifestats.LifeStatsClient;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 시세 관리. prices.json 은 {"품목명": 개당 가격} 형식이다.
 * 화면에서 고른 단위(1개/1세트/1설커)는 units.json 에 따로 저장한다.
 */
public final class PriceManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Double> PER_PIECE = new LinkedHashMap<>();
    private static final Map<String, Integer> UNITS = new HashMap<>();
    private static boolean dirty;

    private PriceManager() {
    }

    public static void load() {
        PER_PIECE.clear();
        UNITS.clear();
        try {
            JsonObject o = JsonParser.parseString(ConfigFiles.readWithDefault("prices.json")).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                PER_PIECE.put(e.getKey(), e.getValue().getAsDouble());
            }
            String u = ConfigFiles.readIfExists("units.json");
            if (u != null) {
                Map<String, Integer> m = GSON.fromJson(u, new TypeToken<Map<String, Integer>>() {
                }.getType());
                if (m != null) UNITS.putAll(m);
            }
        } catch (Exception ex) {
            LifeStatsClient.LOG.error("시세 파일을 읽지 못했습니다", ex);
        }
    }

    public static double perPiece(String name) {
        return PER_PIECE.getOrDefault(name, 0.0);
    }

    public static void setPerPiece(String name, double price) {
        PER_PIECE.put(name, Math.max(0, price));
        dirty = true;
    }

    public static int unit(String name) {
        return UNITS.getOrDefault(name, 1);
    }

    public static void setUnit(String name, int unit) {
        UNITS.put(name, unit);
        dirty = true;
    }

    public static void save() {
        if (!dirty) return;
        JsonObject o = new JsonObject();
        for (Map.Entry<String, Double> e : PER_PIECE.entrySet()) {
            double v = e.getValue();
            if (v == Math.rint(v)) o.addProperty(e.getKey(), (long) v);
            else o.addProperty(e.getKey(), v);
        }
        ConfigFiles.write("prices.json", GSON.toJson(o));
        ConfigFiles.write("units.json", GSON.toJson(UNITS));
        dirty = false;
    }
}
