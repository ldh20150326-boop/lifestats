package com.lifestats.track;

import com.lifestats.data.ItemRegistry;
import com.lifestats.data.StatsStore;
import com.lifestats.model.ItemDef;
import com.lifestats.model.Job;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

/**
 * 인벤토리를 5틱마다 훑어서 등록된 품목의 증가분을 통계에 더한다.
 * 상자·상점 같은 화면이 열려 있는 동안에는 기준점만 갱신해서, 거기서 꺼낸 아이템이 수집량으로 잡히지 않게 한다.
 */
public final class InventoryTracker {
    /** 통계 화면에서 아이콘으로 쓰려고 마지막으로 본 아이템 모양을 기억해 둔다. */
    public static final Map<String, ItemStack> ICONS = new HashMap<>();

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

    /** 색 코드 등 서식을 뗀 표시 이름 */
    public static String nameOf(ItemStack stack) {
        String n = Formatting.strip(stack.getName().getString());
        return n == null ? "" : n.trim();
    }
}
