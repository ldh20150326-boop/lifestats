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

/** 살목지 오리지널 시안 100% 재현 통계 화면 */
public class StatsScreen extends Screen {
    private static final Identifier BG_SALMOKJI = Identifier.of("lifestats", "salmokji.png");

    // 시안 정밀 컬러 팔레트
    private static final int C_OUTER_BG = 0xC8050101;     // 전체 배경 어두운 검붉은색
    private static final int C_CARD_BG = 0xE60D0303;      // 각 카드 내부 배경
    private static final int C_TAG_BG = 0xFF170404;       // 왼쪽 뱃지 태그 배경
    private static final int C_BORDER_RED = 0xFFE01818;   // 시안의 선명한 네온 레드 테두리
    private static final int C_BORDER_DIM = 0xFF6E0D0D;   // 은은한 보조 테두리

    private static final int C_TEXT_WHITE = 0xFFFFFFFF;   // 기본 텍스트
    private static final int C_TEXT_YELLOW = 0xFFFFDE38;  // 수량/수익 강조 노란색
    private static final int C_TEXT_DIM = 0xFF839299;     // 미확인/비활성 회색
    private static final int C_BAR_CYAN = 0xFF00D4FF;     // 상단 막대그래프 아이콘 하늘색

    private static Period lastPeriod = Period.MONTH;
    private static Job lastJob = Job.HUNTER;

    private Period period = lastPeriod;
    private Job job = lastJob;
    private double scroll;
    private int maxScroll;

    private final List<ButtonWidget> periodButtons = new ArrayList<>();
    private final List<ButtonWidget> jobButtons = new ArrayList<>();

    public StatsScreen() {
        super(Text.literal("생활 통계"));
    }

    private int panelW() {
        return Math.min(width - 20, 680);
    }

    private int panelX() {
        return (width - panelW()) / 2;
    }

    @Override
    protected void init() {
        periodButtons.clear();
        jobButtons.clear();

        int w = panelW();
        int x0 = panelX();

        // 1. 기간 탭 버튼 (일간 / 주간 / 월간)
        int px = x0 + 12;
        int pw = 48;
        for (Period p : Period.values()) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(p.label), btn -> {
                period = p;
                lastPeriod = p;
                scroll = 0;
                clearAndInit();
            }).dimensions(px, 34, pw, 17).build();
            addDrawableChild(b);
            periodButtons.add(b);
            px += pw + 4;
        }

        // 2. 시세 입력 버튼 (상단 우측 배치)
        addDrawableChild(ButtonWidget.builder(Text.literal("시세 입력"), btn ->
                client.setScreen(new PriceScreen(this, job))).dimensions(x0 + 185, 12, 60, 16).build());

        // 3. 직업 탭 버튼 (광부 / 농부 / 어부 / 나무꾼 / 사냥꾼)
        Job[] tabs = Job.tabs();
        int jx = x0 + 185;
        int jw = (x0 + w - 12 - jx - (tabs.length - 1) * 4) / tabs.length;
        for (Job j : tabs) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(j.label), btn -> {
                job = j;
                lastJob = j;
                scroll = 0;
                clearAndInit();
            }).dimensions(jx, 34, jw, 17).build();
            addDrawableChild(b);
            jobButtons.add(b);
            jx += jw + 4;
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
        ctx.fill(0, 0, width, height, 0xCC000000);

        // 2. 살목지 일러스트 배경 그리기 (투명도 30% 0x4DFFFFFF)
        try {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, BG_SALMOKJI, x0, 6, 0.0F, 0.0F, w, height - 12, w, height - 12, 0x4DFFFFFF);
        } catch (Exception ignored) {
        }

        // 3. 메인 상단 헤더 카드
        renderHeaderCard(ctx, x0, w);

        // 4. 서브 타이틀 바 (사냥꾼 통계 | 월간 수집 결과)
        int subY = 56;
        ctx.fill(x0 + 6, subY, x0 + w - 6, subY + 16, C_CARD_BG);
        ctx.drawBorder(x0 + 6, subY, w - 12, 16, C_BORDER_RED);
        String subText = job.label + " 통계     |     " + period.label + " 수집 결과 (" + job.label + ")";
        ctx.drawText(textRenderer, subText, x0 + 14, subY + 4, C_TEXT_WHITE, true);

        // 버튼 렌더링
        super.render(ctx, mouseX, mouseY, delta);
        highlightActiveButtons(ctx);

        Map<String, Long> mine = StatsStore.totals(period, job.name());
        Map<String, Long> common = StatsStore.totals(period, Job.COMMON.name());

        // 5. 스크롤 본문 카드 영역
        int top = 76;
        int bottom = height - 36;
        ctx.enableScissor(x0 + 4, top, x0 + w - 4, bottom);
        int y = top - (int) scroll;

        y = drawTaggedCard(ctx, x0, w, y, "원소", mine, job, 3, Category.ELEMENT);
        y = drawTaggedCard(ctx, x0, w, y, "부산물", mine, job, 3,
                Category.BYPRODUCT_RARE, Category.BYPRODUCT_EPIC, Category.CRYSTAL);
        y = drawTaggedCard(ctx, x0, w, y, "유물", mine, job, 4, Category.RELIC);
        y = drawTaggedCard(ctx, x0, w, y, "공통", common, Job.COMMON, 3, Category.COMMON);

        ctx.disableScissor();

        int contentHeight = y - (top - (int) scroll);
        maxScroll = Math.max(0, contentHeight - (bottom - top));
        if (scroll > maxScroll) scroll = maxScroll;

        // 6. 하단 경험치 카드 (독립 태그 카드)
        renderExpCard(ctx, x0, w, height - 32);
    }

    private void renderHeaderCard(DrawContext ctx, int x0, int w) {
        ctx.fill(x0 + 6, 8, x0 + w - 6, 54, C_CARD_BG);
        ctx.drawBorder(x0 + 6, 8, w - 12, 46, C_BORDER_RED);

        // 좌측: 파란색 막대그래프 아이콘
        ctx.fill(x0 + 14, 20, x0 + 17, 24, C_BAR_CYAN);
        ctx.fill(x0 + 19, 17, x0 + 22, 24, C_BAR_CYAN);
        ctx.fill(x0 + 24, 13, x0 + 27, 24, C_BAR_CYAN);

        // 타이틀
        ctx.drawText(textRenderer, "생활 통계", x0 + 33, 15, C_TEXT_WHITE, true);

        // 중앙: 생직 시간
        String timeLabel = "생직 시간";
        String timeVal = Fmt.duration(StatsStore.seconds(period, job.name()));
        ctx.drawText(textRenderer, timeLabel, x0 + 270, 15, C_TEXT_WHITE, false);
        ctx.drawText(textRenderer, timeVal, x0 + 325, 15, C_TEXT_YELLOW, true);

        // 우측: 예상 수익
        String incomeVal = Fmt.won(income(StatsStore.totals(period, job.name()), StatsStore.totals(period, Job.COMMON.name())));
        String incomeText = "예상 수익   |   " + incomeVal;
        ctx.drawText(textRenderer, "예상 수익   |", x0 + w - 200, 15, C_TEXT_WHITE, false);
        ctx.drawText(textRenderer, incomeVal, x0 + w - 20 - textRenderer.getWidth(incomeVal), 15, C_TEXT_YELLOW, true);
    }

    private void highlightActiveButtons(DrawContext ctx) {
        // 선택된 기간 버튼 붉은색 글로우 하이라이트
        for (int i = 0; i < Period.values().length; i++) {
            if (Period.values()[i] == period && i < periodButtons.size()) {
                ButtonWidget b = periodButtons.get(i);
                ctx.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), 0xD0C01010);
                ctx.drawBorder(b.getX(), b.getY(), b.getWidth(), b.getHeight(), C_BORDER_RED);
                int tw = textRenderer.getWidth(b.getMessage());
                ctx.drawText(textRenderer, b.getMessage(), b.getX() + (b.getWidth() - tw) / 2, b.getY() + 5, C_TEXT_WHITE, true);
            }
        }
        // 선택된 직업 버튼 붉은색 글로우 하이라이트
        Job[] tabs = Job.tabs();
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == job && i < jobButtons.size()) {
                ButtonWidget b = jobButtons.get(i);
                ctx.fill(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), 0xD0C01010);
                ctx.drawBorder(b.getX(), b.getY(), b.getWidth(), b.getHeight(), C_BORDER_RED);
                int tw = textRenderer.getWidth(b.getMessage());
                ctx.drawText(textRenderer, b.getMessage(), b.getX() + (b.getWidth() - tw) / 2, b.getY() + 5, C_TEXT_WHITE, true);
            }
        }
    }

    private int drawTaggedCard(DrawContext ctx, int x0, int w, int y, String tag,
                               Map<String, Long> counts, Job source, int cols, Category... cats) {
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

        int rows = (list.size() + cols - 1) / cols;
        int cardH = Math.max(26, rows * 18 + 8);

        int tagW = 46;
        int cardX = x0 + 6;
        int cardW = w - 12;

        // 1. 전체 카드 배경 및 테두리
        ctx.fill(cardX, y, cardX + cardW, y + cardH, C_CARD_BG);
        ctx.drawBorder(cardX, y, cardW, cardH, C_BORDER_RED);

        // 2. 왼쪽 돌출형 뱃지 태그
        ctx.fill(cardX, y, cardX + tagW, y + cardH, C_TAG_BG);
        ctx.fill(cardX + tagW, y, cardX + tagW + 1, y + cardH, C_BORDER_RED); // 구분선

        // 태그 텍스트 중앙 배치
        int tagTw = textRenderer.getWidth(tag);
        ctx.drawText(textRenderer, tag, cardX + (tagW - tagTw) / 2, y + (cardH - 8) / 2, C_TEXT_WHITE, true);

        // 3. 카드 내부 아이템 리스트 (3열/4열 배치)
        int contentX = cardX + tagW + 8;
        int contentW = cardW - tagW - 14;
        int colW = contentW / cols;

        for (int i = 0; i < list.size(); i++) {
            ItemDef d = list.get(i);
            int cx = contentX + (i % cols) * colW;
            int cy = y + 5 + (i / cols) * 18;

            ItemStack icon = InventoryTracker.ICONS.get(d.name());
            if (icon != null) ctx.drawItem(icon, cx, cy);

            long n = counts.getOrDefault(d.name(), 0L);
            String value = n > 0 ? Fmt.count(n) : zeroText(d.category());
            int vw = textRenderer.getWidth(value);
            int nameMax = Math.max(20, colW - 20 - vw - 6);

            ctx.drawText(textRenderer, textRenderer.trimToWidth(d.name(), nameMax), cx + 20, cy + 4, C_TEXT_WHITE, false);
            ctx.drawText(textRenderer, value, cx + colW - 4 - vw, cy + 4, n > 0 ? C_TEXT_YELLOW : C_TEXT_DIM, false);
        }

        return y + cardH + 6; // 다음 카드와의 간격
    }

    private void renderExpCard(DrawContext ctx, int x0, int w, int y) {
        int cardX = x0 + 6;
        int cardW = w - 12;
        int cardH = 24;
        int tagW = 46;

        ctx.fill(cardX, y, cardX + cardW, y + cardH, C_CARD_BG);
        ctx.drawBorder(cardX, y, cardW, cardH, C_BORDER_RED);

        // 왼쪽 경험치 뱃지 태그
        ctx.fill(cardX, y, cardX + tagW, y + cardH, C_TAG_BG);
        ctx.fill(cardX + tagW, y, cardX + tagW + 1, y + cardH, C_BORDER_RED);
        int tagTw = textRenderer.getWidth("경험치");
        ctx.drawText(textRenderer, "경험치", cardX + (tagW - tagTw) / 2, y + 8, C_TEXT_WHITE, true);

        // 경험치 수치 텍스트
        if (client != null && client.player != null) {
            ExpTracker.ensureStarted(client);
            String startStr = "시작 Lv." + ExpTracker.startLevel() + " " + Fmt.percent(ExpTracker.startProgress());
            String curStr = "현재 Lv." + ExpTracker.currentLevel(client) + " " + Fmt.percent(ExpTracker.currentProgress(client));
            String mainExp = startStr + "   →   " + curStr;
            ctx.drawText(textRenderer, mainExp, cardX + tagW + 12, y + 5, C_TEXT_YELLOW, false);

            ctx.drawText(textRenderer, "레벨 곡선 가정값", cardX + tagW + 12, y + 14, C_TEXT_DIM, false);
        }
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
        scroll = Math.max(0, Math.min(maxScroll, scroll - verticalAmount * 16));
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
