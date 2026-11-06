package com.lifestats.screen;

import com.lifestats.data.*; import com.lifestats.model.*; import com.lifestats.track.*; import com.lifestats.util.Fmt;
import net.minecraft.client.gl.RenderPipelines; import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen; import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack; import net.minecraft.text.Text; import net.minecraft.util.Identifier;
import java.util.*;

public class StatsScreen extends Screen {
    private static final Identifier BG = Identifier.of("lifestats", "salmokji.png");
    private static Period period = Period.MONTH; private static Job job = Job.HUNTER;
    private double scroll; private int maxScroll;
    private final List<ButtonWidget> pBtns = new ArrayList<>(), jBtns = new ArrayList<>();

    public StatsScreen() { super(Text.literal("생활 통계")); }
    private int pW() { return Math.min(width - 24, 690); }
    private int pX() { return (width - pW()) / 2; }

    @Override
    protected void init() {
        pBtns.clear(); jBtns.clear();
        int w = pW(), x0 = pX(), px = x0 + 12;
        for (Period p : Period.values()) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(p.label), btn -> { period = p; scroll = 0; clearAndInit(); }).dimensions(px, 32, 48, 16).build();
            addDrawableChild(b); pBtns.add(b); px += 52;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("시세 입력"), btn -> client.setScreen(new PriceScreen(this, job))).dimensions(x0 + 175, 11, 56, 15).build());
        Job[] tabs = Job.tabs();
        int jx = x0 + 175, jw = (x0 + w - 12 - jx - (tabs.length - 1) * 4) / tabs.length;
        for (Job j : tabs) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(j.label), btn -> { job = j; scroll = 0; clearAndInit(); }).dimensions(jx, 32, jw, 16).build();
            addDrawableChild(b); jBtns.add(b); jx += jw + 4;
        }
    }

    @Override public void renderBackground(DrawContext ctx, int mx, int my, float d) {}

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int w = pW(), x0 = pX();
        // 1. 전체 화면 반투명 붉은 틴트
        ctx.fill(0, 0, width, height, 0x882A0202);
        try { ctx.drawTexture(RenderPipelines.GUI_TEXTURED, BG, x0, 5, 0.0F, 0.0F, w, height - 10, w, height - 10, 0x4DFFFFFF); } catch (Exception ignored) {}

        // 2. 상단 헤더 박스 (반투명 붉은색)
        ctx.fill(x0 + 6, 6, x0 + w - 6, 51, 0x80250404); ctx.drawBorder(x0 + 6, 6, w - 12, 45, 0xFFFF1E1E);
        ctx.fill(x0 + 14, 18, x0 + 17, 22, 0xFF00D4FF); ctx.fill(x0 + 19, 15, x0 + 22, 22, 0xFF00D4FF); ctx.fill(x0 + 24, 11, x0 + 27, 22, 0xFF00D4FF);
        ctx.drawText(textRenderer, "생활 통계", x0 + 33, 14, 0xFFFFFFFF, true);
        ctx.drawText(textRenderer, "생직 시간", x0 + 250, 14, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, Fmt.duration(StatsStore.seconds(period, job.name())), x0 + 305, 14, 0xFFFFDF38, true);

        String inc = Fmt.won(income(StatsStore.totals(period, job.name()), StatsStore.totals(period, Job.COMMON.name())));
        int iw = textRenderer.getWidth(inc), rx = x0 + w - 14 - (iw + 90);
        ctx.fill(rx, 9, x0 + w - 12, 26, 0x90450808); ctx.drawBorder(rx, 9, x0 + w - 12 - rx, 17, 0xFF8A1010);
        ctx.drawText(textRenderer, "예상 수익   |", rx + 8, 14, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, inc, x0 + w - 18 - iw, 14, 0xFFFFDF38, true);

        // 3. 서브 타이틀 바 (반투명 붉은색)
        ctx.fill(x0 + 6, 54, x0 + w - 6, 70, 0x80250404); ctx.drawBorder(x0 + 6, 54, w - 12, 16, 0xFFFF1E1E);
        ctx.drawText(textRenderer, job.label + " 통계     |     " + period.label + " 수집 결과 (" + job.label + ")", x0 + 14, 58, 0xFFFFFFFF, true);

        super.render(ctx, mouseX, mouseY, delta);

        // 4. 탭 버튼 네온 렌더
        for (int i = 0; i < Period.values().length && i < pBtns.size(); i++) {
            ButtonWidget b = pBtns.get(i); boolean a = Period.values()[i] == period;
            ctx.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), a ? 0xD0C01010 : 0x80300606);
            ctx.drawBorder(b.getX(), b.getY(), b.getWidth(), b.getHeight(), a ? 0xFFFF1E1E : 0xFF7A1010);
            ctx.drawText(textRenderer, b.getMessage(), b.getX() + (b.getWidth() - textRenderer.getWidth(b.getMessage())) / 2, b.getY() + 4, 0xFFFFFFFF, a);
        }
        Job[] tabs = Job.tabs();
        for (int i = 0; i < tabs.length && i < jBtns.size(); i++) {
            ButtonWidget b = jBtns.get(i); boolean a = tabs[i] == job;
            ctx.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), a ? 0xD0C01010 : 0x80300606);
            ctx.drawBorder(b.getX(), b.getY(), b.getWidth(), b.getHeight(), a ? 0xFFFF1E1E : 0xFF7A1010);
            ctx.drawText(textRenderer, b.getMessage(), b.getX() + (b.getWidth() - textRenderer.getWidth(b.getMessage())) / 2, b.getY() + 4, 0xFFFFFFFF, a);
        }

        Map<String, Long> mine = StatsStore.totals(period, job.name()), com = StatsStore.totals(period, Job.COMMON.name());

        // 5. 카드 스크롤 영역
        int top = 74, bot = height - 36;
        ctx.enableScissor(x0 + 4, top, x0 + w - 4, bot);
        int y = top - (int) scroll;
        y = drawCard(ctx, x0, w, y, "원소", mine, job, 3, false, Category.ELEMENT);
        y = drawCard(ctx, x0, w, y, "부산물", mine, job, 3, false, Category.BYPRODUCT_RARE, Category.BYPRODUCT_EPIC, Category.CRYSTAL);
        y = drawCard(ctx, x0, w, y, "유물", mine, job, 5, true, Category.RELIC);
        y = drawCard(ctx, x0, w, y, "공통", com, Job.COMMON, 3, false, Category.COMMON);
        ctx.disableScissor();

        maxScroll = Math.max(0, (y - (top - (int) scroll)) - (bot - top));
        if (scroll > maxScroll) scroll = maxScroll;

        // 6. 하단 경험치 바 (반투명 붉은색)
        int cX = x0 + 6, cW = w - 12, ey = height - 30;
        ctx.fill(cX, ey, cX + cW, ey + 24, 0x80250404); ctx.drawBorder(cX, ey, cW, 24, 0xFFFF1E1E);
        ctx.fill(cX, ey, cX + 46, ey + 24, 0x99380606); ctx.fill(cX + 46, ey, cX + 47, ey + 24, 0xFFFF1E1E);
        ctx.drawText(textRenderer, "경험치", cX + (46 - textRenderer.getWidth("경험치")) / 2, ey + 8, 0xFFFFFFFF, true);
        if (client != null && client.player != null) {
            ExpTracker.ensureStarted(client);
            String exp = "시작 Lv." + ExpTracker.startLevel() + " " + Fmt.percent(ExpTracker.startProgress()) + "    →    현재 Lv." + ExpTracker.currentLevel(client) + " " + Fmt.percent(ExpTracker.currentProgress(client));
            ctx.drawText(textRenderer, exp, cX + 58, ey + 4, 0xFFFFDF38, true);
            ctx.drawText(textRenderer, "레벨 곡선 가정값", cX + 58, ey + 14, 0xFF839299, false);
            ctx.drawText(textRenderer, "|     획득 경험치   +측정중", cX + cW - 140, ey + 8, 0xFFFFDF38, false);
        }
    }

    private int drawCard(DrawContext ctx, int x0, int w, int y, String tag, Map<String, Long> counts, Job src, int cols, boolean relic, Category... cats) {
        List<ItemDef> list = new ArrayList<>();
        for (ItemDef d : ItemRegistry.items(src)) {
            for (Category c : cats) { if (d.category() == c) { list.add(d); break; } }
        }
        if (list.isEmpty()) return y;

        int rows = (list.size() + cols - 1) / cols, cardH = relic ? 28 : Math.max(26, rows * 18 + 8);
        int cX = x0 + 6, cW = w - 12;

        // 반투명 붉은색 카드 배경 (0x80250404) & 뱃지 (0x99380606)
        ctx.fill(cX, y, cX + cW, y + cardH, 0x80250404); ctx.drawBorder(cX, y, cW, cardH, 0xFFFF1E1E);
        ctx.fill(cX, y, cX + 46, y + cardH, 0x99380606); ctx.fill(cX + 46, y, cX + 47, y + cardH, 0xFFFF1E1E);
        ctx.drawText(textRenderer, tag, cX + (46 - textRenderer.getWidth(tag)) / 2, y + (cardH - 8) / 2, 0xFFFFFFFF, true);

        int inX = cX + 54, inW = cW - 60;
        if (relic) {
            int itemW = inW / Math.max(1, list.size());
            for (int i = 0; i < list.size(); i++) {
                ItemDef d = list.get(i); int ix = inX + i * itemW;
                ItemStack ic = InventoryTracker.ICONS.get(d.name());
                if (ic != null) ctx.drawItem(ic, ix, y + 6);
                long n = counts.getOrDefault(d.name(), 0L);
                ctx.drawText(textRenderer, textRenderer.trimToWidth(d.name(), Math.max(20, itemW - 22)), ix + 20, y + 5, 0xFFFFFFFF, false);
                ctx.drawText(textRenderer, n > 0 ? Fmt.count(n) : "0개", ix + 20, y + 14, n > 0 ? 0xFFFFDF38 : 0xFF839299, false);
            }
        } else {
            int colW = inW / cols;
            for (int i = 0; i < list.size(); i++) {
                ItemDef d = list.get(i); int cx = inX + (i % cols) * colW, cy = y + 5 + (i / cols) * 18;
                ItemStack ic = InventoryTracker.ICONS.get(d.name());
                if (ic != null) ctx.drawItem(ic, cx, cy);
                long n = counts.getOrDefault(d.name(), 0L);
                String val = n > 0 ? Fmt.count(n) : ((d.category() == Category.RELIC || d.category() == Category.COMMON) ? "0개" : "미확인");
                int vw = textRenderer.getWidth(val);
                ctx.drawText(textRenderer, textRenderer.trimToWidth(d.name(), Math.max(20, colW - 20 - vw - 6)), cx + 20, cy + 4, 0xFFFFFFFF, false);
                ctx.drawText(textRenderer, val, cx + colW - 4 - vw, cy + 4, n > 0 ? 0xFFFFDF38 : 0xFF839299, false);
            }
        }
        return y + cardH + 5;
    }

    private static long income(Map<String, Long> mine, Map<String, Long> com) {
        double sum = 0;
        for (Map.Entry<String, Long> e : mine.entrySet()) sum += e.getValue() * PriceManager.perPiece(e.getKey());
        for (Map.Entry<String, Long> e : com.entrySet()) sum += e.getValue() * PriceManager.perPiece(e.getKey());
        return Math.round(sum);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 16)); return true;
    }

    @Override public boolean shouldPause() { return false; }
}
