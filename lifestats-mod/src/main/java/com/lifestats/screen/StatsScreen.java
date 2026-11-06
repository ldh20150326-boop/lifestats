package com.lifestats.screen;

import com.lifestats.data.ItemRegistry;
import com.lifestats.data.PriceManager;
import com.lifestats.data.StatsStore;
import com.lifestats.model.Category;
import com.lifestats.model.ItemDef;
import com.lifestats.model.Job;
import com.lifestats.model.Period;
import com.lifestats.track.ExpTracker;
import com.lifestats.track.InventoryTracker;
import com.lifestats.util.Fmt;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 살목지 오리지널 시안 100% 재현 화면 */
public class StatsScreen extends Screen {
    private static final Identifier BG_SALMOKJI = Identifier.of("lifestats", "salmokji.png");

    private static final int C_BG = 0xDD040000;
    private static final int C_CARD = 0xF50A0202;
    private static final int C_TAG = 0xFF140202;
    private static final int C_RED = 0xFFFF1E1E;
    private static final int C_DARK_RED = 0xFF6E0A0A;
    private static final int C_ACTIVE = 0xFFC00E0E;
    private static final int C_INACTIVE = 0xE0160404;

    private static final int C_WHITE = 0xFFFFFFFF;
    private static final int C_YELLOW = 0xFFFFDF38;
    private static final int C_DIM = 0xFF839299;
    private static final int C_CYAN = 0xFF00D4FF;

    private static Period lastPeriod = Period.MONTH;
    private static Job lastJob = Job.HUNTER;

    private Period period = lastPeriod;
    private Job job = lastJob;
    private double scroll;
    private int maxScroll;

    private final List<ButtonWidget> pBtns = new ArrayList<>();
    private final List<ButtonWidget> jBtns = new ArrayList<>();

    public StatsScreen() {
        super(Text.literal("생활 통계"));
    }

    private int pW() { return Math.min(width - 24, 690); }
    private int pX() { return (width - pW()) / 2; }

    @Override
    protected void init() {
        pBtns.clear();
        jBtns.clear();
        int w = pW(), x0 = pX();

        int px = x0 + 12;
        for (Period p : Period.values()) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(p.label), btn -> {
                period = p; lastPeriod = p; scroll = 0; clearAndInit();
            }).dimensions(px, 32, 48, 16).build();
            addDrawableChild(b);
            pBtns.add(b);
            px += 52;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("시세 입력"), btn ->
                client.setScreen(new PriceScreen(this, job))).dimensions(x0 + 175, 11, 56, 15).build());

        Job[] tabs = Job.tabs();
        int jx = x0 + 175;
        int jw = (x0 + w - 12 - jx - (tabs.length - 1) * 4) / tabs.length;
        for (Job j : tabs) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(j.label), btn -> {
                job = j; lastJob = j; scroll = 0; clearAndInit();
            }).dimensions(jx, 32, jw, 16).build();
            addDrawableChild(b);
            jBtns.add(b);
            jx += jw + 4;
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int w = pW(), x0 = pX();
        ctx.fill(0, 0, width, height, C_BG);

        try {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, BG_SALMOKJI, x0, 5, 0.0F, 0.0F, w, height - 10, w, height - 10, 0x4DFFFFFF);
        } catch (Exception ignored) {}

        // 1. 헤더 박스
        ctx.fill(x0 + 6, 6, x0 + w - 6, 51, C_CARD);
        ctx.drawBorder(x0 + 6, 6, w - 12, 45, C_RED);

        ctx.fill(x0 + 14, 18, x0 + 17, 22, C_CYAN);
        ctx.fill(x0 + 19, 15, x0 + 22, 22, C_CYAN);
        ctx.fill(x0 + 24, 11, x0 + 27, 22, C_CYAN);
        ctx.drawText(textRenderer, "생활 통계", x0 + 33, 14, C_WHITE, true);

        ctx.drawText(textRenderer, "생직 시간", x0 + 250, 14, C_WHITE, false);
        ctx.drawText(textRenderer, Fmt.duration(StatsStore.seconds(period, job.name())), x0 + 305, 14, C_YELLOW, true);

        String inc = Fmt.won(income(StatsStore.totals(period, job.name()), StatsStore.totals(period, Job.COMMON.name())));
        int iw = textRenderer.getWidth(inc);
        int rX = x0 + w - 14 - (iw + 90);
        ctx.fill(rX, 9, x0 + w - 12, 26, 0x90220404);
        ctx.drawBorder(rX, 9, x0 + w - 12 - rX, 17, C_DARK_RED);
        ctx.drawText(textRenderer, "예상 수익   |", rX + 8, 14, C_WHITE, false);
        ctx.drawText(textRenderer, in
