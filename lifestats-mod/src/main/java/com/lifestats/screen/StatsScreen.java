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

/**
 * 스크린샷의 붉은 테두리 및 부산물 구역 레이아웃 전용 화면
 */
public class StatsScreen extends Screen {
    // 스크린샷 테마 색상 (붉은 테두리 + 어두운 흑적색 배경)
    private static final int C_BG_DARK      = 0xD8100707; // 짙은 흑적색 배경
    private static final int C_RED_BORDER   = 0xFFFF1E1E; // 선명한 붉은색 테두리
    private static final int C_TAB_ACTIVE   = 0xFFB31414; // 선택된 탭 붉은 배경
    private static final int C_TEXT_WHITE   = 0xFFFFFFFF; // 흰색 글씨
    private static final int C_TEXT_GOLD    = 0xFFFFD700; // 수량/수익 황금색
    private static final int C_TEXT_DIM     = 0xFFA09696; // 미확인/비활성 회색
    private static final int C_CYAN_ICON    = 0xFF00E5FF; // 상단 그래프 아이콘 청록색

    private static Period lastPeriod = Period.DAY;
    private static Job lastJob = Job.MINER;

    private Period period = lastPeriod;
    private Job job = lastJob;
    private double scroll;
    private int maxScroll;

    public StatsScreen() {
        super(Text.literal("생활 통계"));
    }

    private int panelW() {
        return Math.min(width - 24, 660);
    }

    private int panelX() {
        return (width - panelW()) / 2;
    }

    @Override
    protected void init() {
        int w = panelW();
        int x0 = panelX();

        // 1. 최상단 "시세 입력" 버튼
        addDrawableChild(ButtonWidget.builder(Text.literal("시세 입력"), btn ->
                client.setScreen(new PriceScreen(this, job)))
                .dimensions(x0 + 175, 10, 58, 16).build());

        // 2. [일간] [주간] [월간] 기간 탭 버튼
        int px = x0 + 4;
        for (Period p : Period.values()) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(p.label), btn -> {
                period = p;
                lastPeriod = p;
                scroll = 0;
                clearAndInit();
            }).dimensions(px, 32, 48, 17).build();
            b.active = (p != period);
            addDrawableChild(b);
            px += 51;
        }

        // 3. [광부] [농부] [어부] [나무꾼] [사냥꾼] 직업 탭 버튼
        Job[] tabs = Job.tabs();
        int jx = px + 10;
        int jw = (x0 + w - 4 - jx - (tabs.length - 1) * 3) / tabs.length;
        for (Job j : tabs) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(j.label), btn -> {
                job = j;
                lastJob = j;
                scroll = 0;
                clearAndInit();
            }).dimensions(jx, 32, jw, 17).build();
            b.active = (j != job);
            addDrawableChild(b);
            jx += jw + 3;
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xC0000000);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        int w = panelW();
        int x0 = panelX();

        Map<String, Long> mine = StatsStore.totals(period, job.name());
        Map<String, Long> common = StatsStore.totals(period, Job.COMMON.name());

        // 1. 최상단 바: 생활 통계 / 생직 시간 / 예상 수익
        drawRedBox(ctx, x0, 8, w, 20);
        ctx.drawText(textRenderer, "📊", x0 + 6, 14, C_CYAN_ICON, true);
        ctx.drawText(textRenderer, "생활 통계", x0 + 20, 14, C_TEXT_WHITE, true);

        String timeStr = "생직 시간 " + Fmt.duration(StatsStore.seconds(period, job.name()));
        ctx.drawText(textRenderer, timeStr, x0 + 242, 14, C_TEXT_GOLD, true);

        String incomeVal = Fmt.won(income(mine, common));
        String incomePrefix = "예상 수익   |   ";
        int totalIncomeW = textRenderer.getWidth(incomePrefix + incomeVal);
        ctx.drawText(textRenderer, incomePrefix, x0 + w - 10 - totalIncomeW, 14, C_TEXT_WHITE, true);
        ctx.drawText(textRenderer, incomeVal, x0 + w - 10 - textRenderer.getWidth(incomeVal), 14, C_TEXT_GOLD, true);

        // 2. 탭 외곽 테두리
        drawRedBox(ctx, x0, 30, w, 21);

        // 3. 서브 타이틀 바 (예: 광부 통계 | 일간 수집 결과 <광부>)
        drawRedBox(ctx, x0, 53, w, 18);
        String subTitle = job.label + " 통계   |   " + period.label + " 수집 결과 <" + job.label + ">";
        ctx.drawText(textRenderer, subTitle, x0 + 10, 58, C_TEXT_WHITE, true);

        // 4. 본문 영역 (원소, 부산물, 유물, 공통)
        int top = 74;
        int bottom = height - 38;
        ctx.enableScissor(x0, top, x0 + w, bottom);

        int y = top - (int) scroll;
        // 원소 구역
        y = renderRowSection(ctx, x0, w, y, "원소", mine, job, 3, Category.ELEMENT);
        // 부산물 구역 (스크린샷처럼 3열 그리드 배치)
        y = renderRowSection(ctx, x0, w, y, "부산물", mine, job, 3,
                Category.BYPRODUCT_RARE, Category.BYPRODUCT_EPIC, Category.CRYSTAL);
        // 유물 구역 (광부는 7개 한 줄, 기타 5개)
        int relicCols = w >= 550 ? (job == Job.MINER ? 7 : 5) : 3;
        y = renderRowSection(ctx, x0, w, y, "유물", mine, job, relicCols, Category.RELIC);
        // 공통 구역 (생활의 교본)
        y = renderRowSection(ctx, x0, w, y, "공통", common, Job.COMMON, 3, Category.COMMON);

        ctx.disableScissor();

        // 스크롤 한도 계산
        int contentHeight = y - (top - (int) scroll);
        maxScroll = Math.max(0, contentHeight - (bottom - top));
        if (scroll > maxScroll) scroll = maxScroll;

        // 5. 하단 경험치 바
        renderExpFooter(ctx, x0, height - 34, w);
    }

    /**
     * 좌측 라벨 박스([원소], [부산물] 등) + 우측 아이템 3열 박스 렌더링
     */
    private int renderRowSection(DrawContext ctx, int x0, int w, int y, String label,
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

        int labelW = 46;
        int contentW = w - labelW - 2;
        int rowH = 18;
        int rows = Math.max(1, (list.size() + cols - 1) / cols);
        int totalH = Math.max(22, rows * rowH + 6);

        // 좌측 [부산물] 라벨 박스
        drawRedBox(ctx, x0, y, labelW, totalH);
        int labelTextY = y + (totalH - 8) / 2;
        int labelTextX = x0 + (labelW - textRenderer.getWidth(label)) / 2;
        ctx.drawText(textRenderer, label, labelTextX, labelTextY, C_TEXT_WHITE, true);

        // 우측 부산물 아이템 컨테이너 박스
        int cx0 = x0 + labelW + 2;
        drawRedBox(ctx, cx0, y, contentW, totalH);

        // 아이템 3열 출력
        int colW = (contentW - 12) / cols;
        for (int i = 0; i < list.size(); i++) {
            ItemDef d = list.get(i);
            int col = i % cols;
            int row = i / cols;
            int ix = cx0 + 6 + col * colW;
            int iy = y + 4 + row * rowH;

            // 아이템 아이콘
            ItemStack icon = InventoryTracker.ICONS.get(d.name());
            if (icon != null) {
                ctx.drawItem(icon, ix, iy);
            }

            // 아이템 수량/미확인
            long n = counts.getOrDefault(d.name(), 0L);
            String valStr = n > 0 ? Fmt.count(n) : (d.category() == Category.RELIC || d.category() == Category.COMMON ? "0개" : "미확인");
            int valW = textRenderer.getWidth(valStr);

            // 아이템 이름
            int nameMaxW = Math.max(20, colW - 22 - valW - 4);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(d.name(), nameMaxW), ix + 20, iy + 4, C_TEXT_WHITE, false);
            // 수량 표시
            ctx.drawText(textRenderer, valStr, ix + colW - 4 - valW, iy + 4, n > 0 ? C_TEXT_GOLD : C_TEXT_DIM, false);
        }

        return y + totalH + 3;
    }

    /**
     * 하단 경험치 바 렌더링
     */
    private void renderExpFooter(DrawContext ctx, int x0, int y, int w) {
        int labelW = 46;
        int contentW = w - labelW - 2;
        int h = 26;

        drawRedBox(ctx, x0, y, labelW, h);
        int lx = x0 + (labelW - textRenderer.getWidth("경험치")) / 2;
        ctx.drawText(textRenderer, "경험치", lx, y + (h - 8) / 2, C_TEXT_WHITE, true);

        int cx0 = x0 + labelW + 2;
        drawRedBox(ctx, cx0, y, contentW, h);

        if (client != null && client.player != null) {
            ExpTracker.ensureStarted(client);
            String expMain = "시작 Lv." + ExpTracker.startLevel() + " " + Fmt.percent(ExpTracker.startProgress())
                    + "   ➔   현재 Lv." + ExpTracker.currentLevel(client) + " " + Fmt.percent(ExpTracker.currentProgress(client));
            ctx.drawText(textRenderer, expMain, cx0 + 10, y + 5, C_TEXT_GOLD, false);
            ctx.drawText(textRenderer, "레벨 곡선 가정값", cx0 + 10, y + 15, C_TEXT_DIM, false);

            String status = "획득 경험치  +측정중";
            ctx.drawText(textRenderer, status, cx0 + contentW - 10 - textRenderer.getWidth(status), y + 9, C_TEXT_GOLD, false);
        }
    }

    /**
     * 스크린샷 붉은 테두리 및 배경 그리기
     */
    private void drawRedBox(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, C_BG_DARK);
        ctx.fill(x, y, x + w, y + 1, C_RED_BORDER);
        ctx.fill(x, y + h - 1, x + w, y + h, C_RED_BORDER);
        ctx.fill(x, y, x + 1, y + h, C_RED_BORDER);
        ctx.fill(x + w - 1, y, x + w, y + h, C_RED_BORDER);
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
