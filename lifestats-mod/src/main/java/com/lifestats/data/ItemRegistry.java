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

        // [코드에 5대 직업 모든 품목을 100% 내장 (외부 파일 문제 완전 해결)]
        loadDefaults();

        // 외부 파일이 있다면 추가로 읽기
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
        add(new ItemDef("거미 화석", Job.MINER, Category.RELIC));
        add(new ItemDef("물고기 화석", Job.MINER, Category.RELIC));
        add(new ItemDef("공룡알 화석", Job.MINER, Category.RELIC));
        add(new ItemDef("대지의 정령의 호박화석", Job.MINER, Category.RELIC));
        add(new ItemDef("고대의 피라미드 큐브", Job.MINER, Category.RELIC));
        add(new ItemDef("실종된 광부의 표본샘플", Job.MINER, Category.RELIC));

        // [농부]
        add(new ItemDef("불의 원소 가루", Job.FARMER, Category.ELEMENT));
        add(new ItemDef("불의 원소 조각", Job.FARMER, Category.ELEMENT));
        add(new ItemDef("불의 원소 정수", Job.FARMER, Category.ELEMENT));

        add(new ItemDef("감자 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("당근 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("밀 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("비트 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("수박 조각 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("사탕수수 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("대나무 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("선인장 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("켈프 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("코코아콩 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("달콤한 열매 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("네더와트 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("후렴화 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("갈색 버섯 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("빨간 버섯 번들", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("개량된 목화솜", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("딱정벌레", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("메뚜기", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("무당벌레", Job.FARMER, Category.BYPRODUCT_RARE));
        add(new ItemDef("애벌레", Job.FARMER, Category.BYPRODUCT_RARE));

        add(new ItemDef("고대식물의 모종", Job.FARMER, Category.RELIC));
        add(new ItemDef("실종된 농부의 땅문서", Job.FARMER, Category.RELIC));
        add(new ItemDef("불의 정령의 꽃 화관", Job.FARMER, Category.RELIC));

        // [어부]
        add(new ItemDef("물의 원소 가루", Job.FISHER, Category.ELEMENT));
        add(new ItemDef("물의 원소 조각", Job.FISHER, Category.ELEMENT));
        add(new ItemDef("물의 원소 정수", Job.FISHER, Category.ELEMENT));

        add(new ItemDef("진주", Job.FISHER, Category.BYPRODUCT_RARE));
        add(new ItemDef("마녀의 눈물", Job.FISHER, Category.BYPRODUCT_RARE));
        add(new ItemDef("행운의 편지", Job.FISHER, Category.BYPRODUCT_RARE));

        add(new ItemDef("고대 물의 영혼석", Job.FISHER, Category.RELIC));
        add(new ItemDef("물의 정령의 유리구두", Job.FISHER, Category.RELIC));
        add(new ItemDef("실종된 잠수부 헬멧", Job.FISHER, Category.RELIC));
        add(new ItemDef("용왕의 왕관", Job.FISHER, Category.RELIC));
        add(new ItemDef("해적의 보물상자", Job.FISHER, Category.RELIC));

        // [나무꾼]
        add(new ItemDef("바람의 원소 가루", Job.WOODCUTTER, Category.ELEMENT));
        add(new ItemDef("바람의 원소 조각", Job.WOODCUTTER, Category.ELEMENT));
        add(new ItemDef("바람의 원소 정수", Job.WOODCUTTER, Category.ELEMENT));

        add(new ItemDef("나무껍질", Job.WOODCUTTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("고목나무 수액", Job.WOODCUTTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("바오밥나무 뿌리", Job.WOODCUTTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("맨드레이크", Job.WOODCUTTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("송이버섯", Job.WOODCUTTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("황금버섯", Job.WOODCUTTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("설삼", Job.WOODCUTTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("불로초", Job.WOODCUTTER, Category.BYPRODUCT_RARE));

        add(new ItemDef("고대 정령의 메아리", Job.WOODCUTTER, Category.RELIC));
        add(new ItemDef("나무의 정령의 연꽃 찻잔", Job.WOODCUTTER, Category.RELIC));
        add(new ItemDef("원시부족 가면", Job.WOODCUTTER, Category.RELIC));
        add(new ItemDef("실종된 나무꾼의 장갑", Job.WOODCUTTER, Category.RELIC));
        add(new ItemDef("고급 나무꾼 주머니", Job.WOODCUTTER, Category.RELIC));

        // [사냥꾼]
        add(new ItemDef("영혼의 원소 가루", Job.HUNTER, Category.ELEMENT));
        add(new ItemDef("영혼의 원소 조각", Job.HUNTER, Category.ELEMENT));
        add(new ItemDef("영혼의 원소 정수", Job.HUNTER, Category.ELEMENT));

        add(new ItemDef("물방울의 별", Job.HUNTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("부드러운 깃털", Job.HUNTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("부드러운 털", Job.HUNTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("넘실거리는 불꽃", Job.HUNTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("더러운 천", Job.HUNTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("미스틱 코어", Job.HUNTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("사악한 기운", Job.HUNTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("수상한 주머니", Job.HUNTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("엔더 진주 가루", Job.HUNTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("짐승의 꼬리", Job.HUNTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("짐승의 송곳니", Job.HUNTER, Category.BYPRODUCT_RARE));
        add(new ItemDef("짐승의 가죽", Job.HUNTER, Category.BYPRODUCT_RARE));

        add(new ItemDef("돌고래의 마음", Job.HUNTER, Category.BYPRODUCT_EPIC));
        add(new ItemDef("요정의 가루", Job.HUNTER, Category.BYPRODUCT_EPIC));
        add(new ItemDef("공허의 파동", Job.HUNTER, Category.BYPRODUCT_EPIC));
        add(new ItemDef("용의 피", Job.HUNTER, Category.BYPRODUCT_EPIC));
        add(new ItemDef("버섯 고기", Job.HUNTER, Category.BYPRODUCT_EPIC));
        add(new ItemDef("갈비뼈", Job.HUNTER, Category.BYPRODUCT_EPIC));
        add(new ItemDef("가스트의 파편", Job.HUNTER, Category.BYPRODUCT_EPIC));
        add(new ItemDef("타락한 요정의 가루", Job.HUNTER, Category.BYPRODUCT_EPIC));
        add(new ItemDef("타락한 월계수 잎", Job.HUNTER, Category.BYPRODUCT_EPIC));

        add(new ItemDef("대지의 결정체", Job.HUNTER, Category.CRYSTAL));
        add(new ItemDef("불의 결정체", Job.HUNTER, Category.CRYSTAL));
        add(new ItemDef("물의 결정체", Job.HUNTER, Category.CRYSTAL));
        add(new ItemDef("바람의 결정체", Job.HUNTER, Category.CRYSTAL));
        add(new ItemDef("빛의 결정체", Job.HUNTER, Category.CRYSTAL));
        add(new ItemDef("어둠의 결정체", Job.HUNTER, Category.CRYSTAL));

        add(new ItemDef("신비한 동물 가죽", Job.HUNTER, Category.RELIC));
        add(new ItemDef("실종된 사냥꾼 고글", Job.HUNTER, Category.RELIC));
        add(new ItemDef("고대 맘모스 뿔", Job.HUNTER, Category.RELIC));
        add(new ItemDef("어둠의 정령의 부러진 스태프", Job.HUNTER, Category.RELIC));
        add(new ItemDef("고급 사냥꾼 주머니", Job.HUNTER, Category.RELIC));
    }

    private static void add(ItemDef def) {
        BY_NAME.putIfAbsent(def.name(), def);
        List<ItemDef> list = BY_JOB.get(def.job());
        if (list != null && !list.contains(def)) {
            list.add(def);
        }
    }

    public static ItemDef byName(String name) {
        return BY_NAME.get(name);
    }

    public static List<ItemDef> items(Job job) {
        return BY_JOB.getOrDefault(job, List.of());
    }
}
