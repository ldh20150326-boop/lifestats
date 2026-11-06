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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 살목지 다크 레드 테마 생활 통계 화면 */
public class StatsScreen extends Screen {
    private static final Identifier BG_SALMOKJI = Identifier.of("lifestats", "salmokji.png");

    // 살목지 핏빛/검붉은 테마 컬러 팔레트
    private static final int C_PANEL = 0xD0120505;   // 짙은 검붉은 배경
    private static final int C_BORDER = 0xFF9E1B1B;  // 핏빛 테두리
    private static final int C_TITLE = 0xFFFF3333;   // 살목지 붉은 타이틀
    private static final int C_SECTION = 0xFFFF6666; // 섹션 소제목
    private static final int C_NAME = 0xFFF0E4E4;    // 아이템 이름
    private static final int C_VALUE = 0xFFFFB347;   // 수량/수익 (붉은 노을빛 오렌지 골드)
    private static final int C_DIM = 0xFF9E7070;     // 비활성 텍스트

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
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int w = panelW();
        int x0 = panelX();

        // 1. 전체 화면 어둡게 블러
        ctx.fill(0, 0, width, height, 0xB8000000);

        // 2. 패널 기본 검붉은 바탕
        ctx.fill(x0, 8, x0 + w, height - 8, C_PANEL);

        // 3. 살목지 일러스트 배경 그리기 (1.21.8 최신 규격 투명도 30% 적용)
        try {
            ctx.drawTexture(RenderLayer::getGuiTranslucent, BG_SALMOKJI, x0, 8, 0.0F, 0.0F, w, height - 16, w, height - 16, 0x4DFFFFFF);
        } catch (Exception ignored) {
        }

        // 4. 살목지 핏빛 테두리
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

        // 본문 스크롤 영역
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
