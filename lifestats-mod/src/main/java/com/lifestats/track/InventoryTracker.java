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

/**
 * 인벤토리를 5틱마다 훑어서 등록된 품목의 증가분을 통계에 더한다.
 */
public final class InventoryTracker {
    /** 통계 화면에서 아이콘으로 쓸 아이템 모양 (인벤토리에 없어도 기본 아이콘 자동 표시) */
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

    /** 인벤토리에 아이템이 아직 없을 때 이름에 맞춰 띄워줄 예쁜 기본 마크 아이콘 */
    public static ItemStack getDefaultIcon(String name) {
        Item item = switch (name) {
            case "영혼의 원소 가루" -> Items.GLOWSTONE_DUST;
            case "영혼의 원소 조각" -> Items.PRISMARINE_SHARD;
            case "영혼의 원소 정수" -> Items.NETHER_STAR;
            case "물방울의 별" -> Items.PRISMARINE_CRYSTALS;
            case "부드러운 깃털" -> Items.FEATHER;
            case "부드러운 털" -> Items.RABBIT_HIDE;
            case "넘실거리는 불꽃" -> Items.BLAZE_POWDER;
            case "더러운 천" -> Items.PAPER;
            case "미스틱 코어", "돌고래의 마음" -> Items.HEART_OF_THE_SEA;
            case "사악한 기운" -> Items.FERMENTED_SPIDER_EYE;
            case "수상한 주머니", "고급 사냥꾼 주머니" -> Items.BUNDLE;
            case "요정의 가루" -> Items.SUGAR;
            case "공허의 파동" -> Items.ENDER_EYE;
            case "용의 피" -> Items.DRAGON_BREATH;
            case "버섯 고기" -> Items.COOKED_BEEF;
            case "갈비뼈" -> Items.BONE;
            case "가스트의 파편" -> Items.GHAST_TEAR;
            case "타락한 요정의 가루" -> Items.REDSTONE;
            case "대지의 결정체" -> Items.EMERALD;
            case "불의 결정체" -> Items.MAGMA_CREAM;
            case "물의 결정체" -> Items.LAPIS_LAZULI;
            case "바람의 결정체" -> Items.DIAMOND;
            case "빛의 결정체" -> Items.GOLD_INGOT;
            case "어둠의 결정체" -> Items.ECHO_SHARD;
            case "신비한 동물 가죽" -> Items.LEATHER;
            case "실종된 사냥꾼 고글" -> Items.SPYGLASS;
            case "고대 맘모스 뿔" -> Items.GOAT_HORN;
            case "어둠의 정령의 부러진 스태프" -> Items.BLAZE_ROD;
            case "생활의 교본" -> Items.ENCHANTED_BOOK;
            default -> {
                if (name.contains("가루")) yield Items.GUNPOWDER;
                if (name.contains("조각")) yield Items.PRISMARINE_SHARD;
                if (name.contains("정수")) yield Items.NETHER_STAR;
                if (name.contains("결정체")) yield Items.AMETHYST_SHARD;
                if (name.contains("원석")) yield Items.RAW_IRON;
                if (name.contains("주머니")) yield Items.BUNDLE;
                if (name.contains("번들")) yield Items.HAY_BLOCK;
                if (name.contains("책") || name.contains("교본") || name.contains("문서")) yield Items.BOOK;
                yield Items.EXPERIENCE_BOTTLE;
            }
        };
        return new ItemStack(item);
    }
}
