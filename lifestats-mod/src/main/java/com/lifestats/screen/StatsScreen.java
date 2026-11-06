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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 생활 통계 화면: 기간(일/주/월) × 직업 탭, 구역별 수집량, 예상 수익, 생직 시간, 경험치. */
public class StatsScreen extends Screen {
    private static final int C_PANEL = 0xFF0E2433;
    private static final int C_BORDER = 0xFF1F8FB5;
    private static final int C_TITLE = 0xFFFFFFFF;
    private static final int C_SECTION = 0xFF58C7F3;
    private static final int C_NAME = 0xFFE6EEF2;
    private static final int C_VALUE = 0xFFFFE04D;
    private static final int C_DIM = 0xFF8FA3AE;

    private static Period lastPeriod = Period.MONTH;
    private static Job lastJob = Job.HUNTER;

    private Period period = lastPeriod;
    private Job job = lastJob;
    private double scroll;
    private int maxScroll;

    public StatsScreen() {
        super(Text.literal("생활 통계"));
    }

    private int panelW() {
        return Math.min(width - 16, 560);
    }

    private int panelX() {
        return (width - panelW()) / 2;
    }

    @Override
    protected void init() {
        int w = panelW();
        int x0 = panelX();

        int bx = x0 + 8;
        for (Period p : Period.values()) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(p.label), btn -> {
                period = p;
                lastPeriod = p;
                scroll = 0;
                clearAndInit();
            }).dimensions(bx, 26, 40, 18).build();
            b.active = p != period;
            addDrawableChild(b);
            bx += 44;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("시세 입력"), btn ->
                client.setScreen(new PriceScreen(this, job))).dimensions(x0 + w - 68, 26, 60, 18).build());

        Job[] tabs = Job.tabs();
        int tw = (w - 16 - (tabs.length - 1) * 4) / tabs.length;
        int jx = x0 + 8;
        for (Job j : tabs) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(j.label), btn -> {
                job = j;
                lastJob = j;
                scroll = 0;
                clearAndInit();
            }).dimensions(jx, 48, tw, 18).build();
            b.active = j != job;
            addDrawableChild(b);
            jx += tw + 4;
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 기본 블러 배경 대신 직접 그린다 (render 안에서 처리)
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int w = panelW();
        int x0 = panelX();

        ctx.fill(0, 0, width, height, 0xB0000000);
        ctx.fill(x0, 8, x0 + w, height - 8, C_PANEL);
        ctx.drawBorder(x0, 8, w, height - 16, C_BORDER);

        super.render(ctx, mouseX, mouseY, delta);

        Map<String, Long> mine = StatsStore.totals(period, job.name());
        Map<String, Long> common = StatsStore.totals(period, Job.COMMON.name());

        // 머리글: 제목 / 생직 시간 / 예상 수익
        ctx.drawText(textRenderer, "생활 통계", x0 + 8, 13, C_TITLE, true);
        String time = "생직 시간 " + Fmt.duration(StatsStore.seconds(period, job.name()));
        ctx.drawText(textRenderer, time, x0 + 90, 13, C_VALUE, true);
        String income = "예상 수익 " + Fmt.won(income(mine, common));
        ctx.drawText(textRenderer, income, x0 + w - 8 - textRenderer.getWidth(income), 13, C_VALUE, true);

        // 스크롤되는 본문
        int top = 72;
        int bottom = height - 30;
        ctx.enableScissor(x0 + 4, top, x0 + w - 4, bottom);
        int y = top - (int) scroll;
        y = drawSection(ctx, x0, w, y, "원소", mine, job, Category.ELEMENT);
        y = drawSection(ctx, x0, w, y, "부산물", mine, job,
                Category.BYPRODUCT_RARE, Category.BYPRODUCT_EPIC, Category.CRYSTAL);
        y = drawSection(ctx, x0, w, y, "유물", mine, job, Category.RELIC);
        y = drawSection(ctx, x0, w, y, "공통", common, Job.COMMON, Category.COMMON);
        ctx.disableScissor();

        int contentHeight = y - (top - (int) scroll);
        maxScroll = Math.max(0, contentHeight - (bottom - top));
        if (scroll > maxScroll) scroll = maxScroll;

        if (ItemRegistry.items(job).isEmpty()) {
            String msg = job.label + " 품목이 아직 없습니다. config/lifestats/jobs.json 에 추가하세요.";
            ctx.drawText(textRenderer, msg, x0 + 12, top + 6, C_DIM, false);
        }

        // 하단: 경험치
        ctx.fill(x0 + 4, height - 28, x0 + w - 4, height - 27, C_BORDER);
        ctx.drawText(textRenderer, expLine(), x0 + 8, height - 20, C_NAME, false);
    }

    private String expLine() {
        if (client == null || client.player == null) return "경험치 정보를 읽을 수 없습니다";
        ExpTracker.ensureStarted(client);
        return "경험치  시작 Lv." + ExpTracker.startLevel() + " " + Fmt.percent(ExpTracker.startProgress())
                + "  →  현재 Lv." + ExpTracker.currentLevel(client) + " " + Fmt.percent(ExpTracker.currentProgress(client));
    }

    private int drawSection(DrawContext ctx, int x0, int w, int y, String title,
                            Map<String, Long> counts, Job source, Category... cats) {
        List<ItemDef> list = new ArrayList<>();
        for (ItemDef d : ItemRegistry.items(source)) {
            for (Category c : cats) {
                if (d.category() == c) {
                    list.add(d);
                    break;
                }
            }
        }
        if (list.isEmpty()) return y;

        ctx.drawText(textRenderer, title, x0 + 10, y + 2, C_SECTION, true);
        y += 14;

        int cols = w >= 420 ? 3 : 2;
        int colW = (w - 20) / cols;
        for (int i = 0; i < list.size(); i++) {
            ItemDef d = list.get(i);
            int cx = x0 + 10 + (i % cols) * colW;
            int cy = y + (i / cols) * 18;

            ItemStack icon = InventoryTracker.ICONS.get(d.name());
            if (icon != null) ctx.drawItem(icon, cx, cy);

            long n = counts.getOrDefault(d.name(), 0L);
            String value = n > 0 ? Fmt.count(n) : zeroText(d.category());
            int vw = textRenderer.getWidth(value);
            int nameMax = Math.max(20, colW - 20 - vw - 8);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(d.name(), nameMax), cx + 20, cy + 4, C_NAME, false);
            ctx.drawText(textRenderer, value, cx + colW - 6 - vw, cy + 4, n > 0 ? C_VALUE : C_DIM, false);
        }
        int rows = (list.size() + cols - 1) / cols;
        return y + rows * 18 + 6;
    }

    private static String zeroText(Category c) {
        return (c == Category.RELIC || c == Category.COMMON) ? "0개" : "미확인";
    }

    private static long income(Map<String, Long> mine, Map<String, Long> common) {
        double sum = 0;
        for (Map.Entry<String, Long> e : mine.entrySet()) sum += e.getValue() * PriceManager.perPiece(e.getKey());
        for (Map.Entry<String, Long> e : common.entrySet()) sum += e.getValue() * PriceManager.perPiece(e.getKey());
        return Math.round(sum);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 14));
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
