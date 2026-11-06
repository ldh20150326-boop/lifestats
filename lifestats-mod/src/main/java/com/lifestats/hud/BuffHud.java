package com.lifestats.hud;

import com.lifestats.buff.BuffDef;
import com.lifestats.buff.BuffManager;
import com.lifestats.util.Fmt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** 화면 왼쪽 위에 효과 남은 시간과 재사용 대기 시간을 그린다. 켜져 있는 항목만 표시. */
public final class BuffHud {
    private static final int X = 6;
    private static final int Y = 40;
    private static final int LINE = 11;
    private static final int COL_NAME = 0xFFFFFFFF;
    private static final int COL_EFFECT = 0xFF9FE8B4;
    private static final int COL_COOLDOWN = 0xFFF2C27A;
    private static final int COL_DIM = 0xFFB8C4CC;

    private static boolean visible = true;

    private BuffHud() {
    }

    public static void toggle() {
        visible = !visible;
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!visible || mc.player == null || mc.options.hudHidden) return;

        int y = Y;
        boolean titled = false;
        for (BuffDef d : BuffManager.defs()) {
            long effect = BuffManager.effectLeftSec(d);
            long cooldown = BuffManager.cooldownLeftSec(d);
            if (effect <= 0 && cooldown <= 0) continue;

            if (!titled) {
                ctx.drawText(mc.textRenderer, "버프·재사용", X, y, COL_DIM, true);
                y += LINE + 2;
                titled = true;
            }
            ctx.drawText(mc.textRenderer, d.name, X, y, COL_NAME, true);
            String eff = effect > 0 ? Fmt.mmss(effect) : "종료";
            ctx.drawText(mc.textRenderer, eff, X + 110, y, effect > 0 ? COL_EFFECT : COL_DIM, true);
            if (cooldown > 0) {
                ctx.drawText(mc.textRenderer, Fmt.mmss(cooldown), X + 150, y, COL_COOLDOWN, true);
            }
            y += LINE;
        }
    }
}
