package com.lifestats.data;

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

    private ItemRegistry() {}

    public static void load() {
        BY_NAME.clear();
        BY_JOB.clear();
        for (Job j : Job.values()) BY_JOB.put(j, new ArrayList<>());

        reg(Job.COMMON, Category.COMMON, "생활의 교본");

        reg(Job.MINER, Category.ELEMENT, "땅의 원소 가루", "땅의 원소 조각", "땅의 원소 정수");
        reg(Job.MINER, Category.BYPRODUCT_RARE, "솔라이트 원석", "가넷", "토파즈", "페리도트", "아쿠아마린");
        reg(Job.MINER, Category.RELIC, "암모나이트 화석", "거미 화석", "물고기 화석", "공룡알 화석", "대지의 정령의 호박화석", "고대의 피라미드 큐브", "실종된 광부의 표본샘플");

        reg(Job.FARMER, Category.ELEMENT, "불의 원소 가루", "불의 원소 조각", "불의 원소 정수");
        reg(Job.FARMER, Category.BYPRODUCT_RARE, "감자 번들", "당근 번들", "밀 번들", "비트 번들", "수박 조각 번들", "사탕수수 번들", "대나무 번들", "선인장 번들", "켈프 번들", "코코아콩 번들", "달콤한 열매 번들", "네더와트 번들", "후렴화 번들", "갈색 버섯 번들", "빨간 버섯 번들", "개량된 목화솜", "딱정벌레", "메뚜기", "무당벌레", "애벌레");
        reg(Job.FARMER, Category.RELIC, "고대식물의 모종", "실종된 농부의 땅문서", "불의 정령의 꽃 화관");

        reg(Job.FISHER, Category.ELEMENT, "물의 원소 가루", "물의 원소 조각", "물의 원소 정수");
        reg(Job.FISHER, Category.BYPRODUCT_RARE, "진주", "마녀의 눈물", "행운의 편지");
        reg(Job.FISHER, Category.RELIC, "고대 물의 영혼석", "물의 정령의 유리구두", "실종된 잠수부 헬멧", "용왕의 왕관", "해적의 보물상자");

        reg(Job.WOODCUTTER, Category.ELEMENT, "바람의 원소 가루", "바람의 원소 조각", "바람의 원소 정수");
        reg(Job.WOODCUTTER, Category.BYPRODUCT_RARE, "나무껍질", "고목나무 수액", "바오밥나무 뿌리", "맨드레이크", "송이버섯", "황금버섯", "설삼", "불로초");
        reg(Job.WOODCUTTER, Category.RELIC, "고대 정령의 메아리", "나무의 정령의 연꽃 찻잔", "원시부족 가면", "실종된 나무꾼의 장갑", "고급 나무꾼 주머니");

        reg(Job.HUNTER, Category.ELEMENT, "영혼의 원소 가루", "영혼의 원소 조각", "영혼의 원소 정수");
        reg(Job.HUNTER, Category.BYPRODUCT_RARE, "물방울의 별", "부드러운 깃털", "부드러운 털", "넘실거리는 불꽃", "더러운 천", "미스틱 코어", "사악한 기운", "수상한 주머니", "엔더 진주 가루", "짐승의 꼬리", "짐승의 송곳니", "짐승의 가죽", "독침");
        reg(Job.HUNTER, Category.BYPRODUCT_EPIC, "돌고래의 마음", "요정의 가루", "공허의 파동", "용의 피", "버섯 고기", "갈비뼈", "가스트의 파편", "타락한 요정의 가루", "타락한 월계수 잎");
        reg(Job.HUNTER, Category.CRYSTAL, "대지의 결정체", "불의 결정체", "물의 결정체", "바람의 결정체", "빛의 결정체", "어둠의 결정체");
        reg(Job.HUNTER, Category.RELIC, "신비한 동물 가죽", "실종된 사냥꾼 고글", "고대 맘모스 뿔", "어둠의 정령의 부러진 스태프", "고급 사냥꾼 주머니");
    }

    private static void reg(Job job, Category cat, String... names) {
        for (String name : names) {
            ItemDef def = new ItemDef(name, job, cat);
            BY_NAME.putIfAbsent(name, def);
            BY_JOB.computeIfAbsent(job, k -> new ArrayList<>()).add(def);
        }
    }

    public static ItemDef byName(String name) {
        return BY_NAME.get(name);
    }

    public static List<ItemDef> items(Job job) {
        return BY_JOB.getOrDefault(job, List.of());
    }
}
