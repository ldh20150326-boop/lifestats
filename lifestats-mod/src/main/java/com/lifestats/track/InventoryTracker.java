package com.lifestats.track;

import com.lifestats.data.ItemRegistry;
import com.lifestats.data.StatsStore;
import com.lifestats.model.ItemDef;
import com.lifestats.model.Job;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

public final class InventoryTracker {
    public static final Map<String, ItemStack> ICONS = new HashMap<>() {
        @Override
        public ItemStack get(Object key) {
            ItemStack s = super.get(key);
            if (s != null && !s.isEmpty()) return s;
            if (key instanceof String name) {
                return getDefaultIcon(name);
            }
            return null;
        }
    };

    private static Map<String, Integer> last;
    private static int tick;

    private InventoryTracker() {
    }

    public static void onTick(MinecraftClient mc) {
        if (mc.player == null) {
            last = null;
            return;
        }
        if (++tick % 5 != 0) return;

        Map<String, Integer> now = snapshot(mc.player);
        if (last == null || mc.currentScreen != null) {
            last = now;
            return;
        }
        for (Map.Entry<String, Integer> e : now.entrySet()) {
            int gain = e.getValue() - last.getOrDefault(e.getKey(), 0);
            if (gain <= 0) continue;
            ItemDef def = ItemRegistry.byName(e.getKey());
            if (def == null) continue;
            StatsStore.add(def.job().name(), def.name(), gain);
            if (def.job() != Job.COMMON) PlayTimer.noteGain(def.job());
        }
        last = now;
    }

    private static Map<String, Integer> snapshot(PlayerEntity player) {
        Map<String, Integer> counts = new HashMap<>();
        int size = player.getInventory().size();
        for (int i = 0; i < size; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isEmpty()) continue;
            String name = nameOf(s);
            ItemDef def = ItemRegistry.byName(name);
            if (def == null) continue;
            counts.merge(name, s.getCount(), Integer::sum);
            ICONS.put(name, s.copyWithCount(1));
        }
        return counts;
    }

    public static String nameOf(ItemStack stack) {
        String n = Formatting.strip(stack.getName().getString());
        return n == null ? "" : n.trim();
    }

    public static ItemStack getDefaultIcon(String name) {
        Item item = switch (name) {
            // [사냥꾼 품목 정밀 매칭]
            case "영혼의 원소 가루" -> Items.REDSTONE;
            case "영혼의 원소 조각" -> Items.AMETHYST_SHARD;
            case "영혼의 원소 정수" -> Items.AMETHYST_CLUSTER;
            case "엔더 진주 가루" -> Items.GLOWSTONE_DUST;
            case "요정의 가루" -> Items.SUGAR;
            case "타락한 월계수 잎" -> Items.RED_TULIP;
            case "짐승의 송곳니" -> Items.POINTED_DRIPSTONE;
            case "짐승의 꼬리" -> Items.STICK;
            case "짐승의 가죽" -> Items.RABBIT_HIDE;
            case "신비한 동물 가죽" -> Items.LEATHER;
            case "부드러운 깃털" -> Items.FEATHER;
            case "부드러운 털" -> Items.WHITE_WOOL;
            case "물방울의 별" -> Items.PRISMARINE_CRYSTALS;
            case "돌고래의 마음" -> Items.HEART_OF_THE_SEA;
            case "미스틱 코어" -> Items.ENDER_PEARL;
            case "공허의 파동" -> Items.ENDER_EYE;
            case "넘실거리는 불꽃" -> Items.FIRE_CHARGE;
            case "사악한 기운" -> Items.FERMENTED_SPIDER_EYE;
            case "수상한 주머니", "고급 사냥꾼 주머니" -> Items.BUNDLE;
            case "버섯 고기" -> Items.COOKED_BEEF;
            case "갈비뼈" -> Items.BONE;
            case "가스트의 파편" -> Items.GHAST_TEAR;
            case "용의 피" -> Items.DRAGON_BREATH;
            case "실종된 사냥꾼 고글" -> Items.SPYGLASS;
            case "고대 맘모스 뿔" -> Items.GOAT_HORN;
            case "어둠의 정령의 부러진 스태프" -> Items.BLAZE_ROD;
            case "더러운 천" -> Items.PAPER;
            case "타락한 요정의 가루" -> Items.REDSTONE;

            // [결정체 6종]
            case "대지의 결정체" -> Items.EMERALD;
            case "불의 결정체" -> Items.MAGMA_CREAM;
            case "물의 결정체" -> Items.LAPIS_LAZULI;
            case "바람의 결정체" -> Items.DIAMOND;
            case "빛의 결정체" -> Items.GOLD_INGOT;
            case "어둠의 결정체" -> Items.ECHO_SHARD;

            // [어부 품목]
            case "물의 원소 가루" -> Items.LIGHT_BLUE_DYE;
            case "물의 원소 조각" -> Items.PRISMARINE_SHARD;
            case "물의 원소 정수" -> Items.NETHER_STAR;
            case "진주" -> Items.ENDER_PEARL;
            case "마녀의 눈물" -> Items.GHAST_TEAR;
            case "행운의 편지" -> Items.GLASS_BOTTLE;
            case "고대 물의 영혼석" -> Items.HEART_OF_THE_SEA;
            case "물의 정령의 유리구두" -> Items.DIAMOND_BOOTS;
            case "실종된 잠수부 헬멧" -> Items.TURTLE_HELMET;
            case "용왕의 왕관" -> Items.GOLDEN_HELMET;
            case "해적의 보물상자" -> Items.CHEST;

            // [농부 품목]
            case "불의 원소 가루" -> Items.BLAZE_POWDER;
            case "불의 원소 조각" -> Items.PRISMARINE_SHARD;
            case "불의 원소 정수" -> Items.NETHER_STAR;
            case "감자 번들" -> Items.POTATO;
            case "당근 번들" -> Items.CARROT;
            case "밀 번들" -> Items.HAY_BLOCK;
            case "비트 번들" -> Items.BEETROOT;
            case "수박 조각 번들" -> Items.MELON_SLICE;
            case "사탕수수 번들" -> Items.SUGAR_CANE;
            case "대나무 번들" -> Items.BAMBOO;
            case "선인장 번들" -> Items.CACTUS;
            case "켈프 번들" -> Items.KELP;
            case "코코아콩 번들" -> Items.COCOA_BEANS;
            case "달콤한 열매 번들" -> Items.SWEET_BERRIES;
            case "네더와트 번들" -> Items.NETHER_WART;
            case "후렴화 번들" -> Items.CHORUS_FRUIT;
            case "갈색 버섯 번들" -> Items.BROWN_MUSHROOM;
            case "빨간 버섯 번들" -> Items.RED_MUSHROOM;
            case "개량된 목화솜" -> Items.WHITE_WOOL;
            case "딱정벌레" -> Items.FERMENTED_SPIDER_EYE;
            case "메뚜기" -> Items.WHEAT_SEEDS;
            case "무당벌레" -> Items.SPIDER_EYE;
            case "애벌레" -> Items.SLIME_BALL;
            case "고대식물의 모종" -> Items.TORCHFLOWER_SEEDS;
            case "실종된 농부의 땅문서" -> Items.MAP;
            case "불의 정령의 꽃 화관" -> Items.SUNFLOWER;

            // [나무꾼 품목]
            case "고대 정령의 메아리" -> Items.ECHO_SHARD;
            case "고목나무 수액" -> Items.HONEY_BOTTLE;
            case "나무껍질" -> Items.OAK_LOG;
            case "나무의 정령의 연꽃 찻잔" -> Items.FLOWER_POT;
            case "맨드레이크" -> Items.FERN;
            case "바람의 원소 가루" -> Items.SUGAR;
            case "바람의 원소 정수" -> Items.NETHER_STAR;
            case "바람의 원소 조각" -> Items.PRISMARINE_CRYSTALS;
            case "바오밥나무 뿌리" -> Items.MANGROVE_ROOTS;
            case "불로초" -> Items.TORCHFLOWER;
            case "설삼" -> Items.HANGING_ROOTS;
            case "송이버섯" -> Items.BROWN_MUSHROOM;
            case "실종된 나무꾼의 장갑" -> Items.LEATHER;
            case "원시부족 가면" -> Items.SKELETON_SKULL;
            case "황금버섯" -> Items.GOLDEN_CARROT;
            case "고급 나무꾼 주머니" -> Items.BUNDLE;

            // [광부 품목]
            case "아쿠아마린" -> Items.DIAMOND;
            case "암모나이트 화석" -> Items.NAUTILUS_SHELL;
            case "토파즈" -> Items.RAW_GOLD;
            case "페리도트" -> Items.EMERALD;
            case "가넷" -> Items.REDSTONE;
            case "거미 화석" -> Items.COBWEB;
            case "고대의 피라미드 큐브" -> Items.RESPAWN_ANCHOR;
            case "공룡알 화석" -> Items.SNIFFER_EGG;
            case "대지의 정령의 호박화석" -> Items.HONEYCOMB;
            case "땅의 원소 가루" -> Items.GUNPOWDER;
            case "땅의 원소 조각" -> Items.PRISMARINE_SHARD;
            case "땅의 원소 정수" -> Items.NETHER_STAR;
            case "물고기 화석" -> Items.BONE;
            case "솔라이트 원석" -> Items.AMETHYST_CLUSTER;
            case "실종된 광부의 표본샘플" -> Items.GLASS_BOTTLE;

            // [공통]
            case "생활의 교본" -> Items.ENCHANTED_BOOK;

            default -> {
                if (name.contains("가루")) yield Items.GUNPOWDER;
                if (name.contains("조각")) yield Items.PRISMARINE_SHARD;
                if (name.contains("정수")) yield Items.NETHER_STAR;
                if (name.contains("결정체")) yield Items.AMETHYST_SHARD;
                if (name.contains("원석")) yield Items.RAW_IRON;
                if (name.contains("화석")) yield Items.BONE;
                if (name.contains("주머니")) yield Items.BUNDLE;
                if (name.contains("버섯")) yield Items.BROWN_MUSHROOM;
                if (name.contains("번들")) yield Items.HAY_BLOCK;
                if (name.contains("책") || name.contains("교본") || name.contains("문서")) yield Items.BOOK;
                yield Items.EXPERIENCE_BOTTLE;
            }
        };
        return new ItemStack(item);
    }
}
