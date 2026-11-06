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

/** 시안 디자인 100% 정밀 재현 생활 통계 화면 */
public class StatsScreen extends Screen {
    private static final Identifier BG_SALMOKJI = Identifier.of("lifestats", "salmokji.png");

    // 시안 정밀 네온 레드/검붉은 컬러 팔레트
    private static final int C_OVERLAY_BG = 0xDD040000;    // 전체 배경 깊은 칠흑빛
    private static final int C_BOX_BG = 0xF50A0202;        // 카드 내부 배경
    private static final int C_TAG_BG = 0xFF140202;        // 왼쪽 뱃지 태그 배경
    private static final int C_NEON_RED = 0xFFFF1E1E;      // 시안의 쨍한 네온 레드 테두리
    private static final int C_DARK_RED = 0xFF6E0A0A;      // 어두운 보조 붉은선
    private static final int C_ACTIVE_RED = 0xFFC00E0E;    // 선택된 탭 붉은 네온 채우기
    private static final int C_INACTIVE_BTN = 0xE0160404;  // 미선택 탭 어두운 배경

    private static final int C_TEXT_WHITE = 0xFFFFFFFF;    // 기본 흰색 텍스트
    private static final int C_TEXT_YELLOW = 0xFFFFDF38;   // 시안 특유의 쨍한 수량/수익 노란색
    private static final int C_TEXT_DIM = 0xFF839299;      // 미확인/비활성 회색
    private static final int C_CYAN = 0xFF00D4FF;          // 상단 막대그래프 청록색

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
        return Math.min(width - 24, 690);
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
            }).dimensions(px, 32, pw, 16).build();
            addDrawableChild(b);
            periodButtons.add(b);
            px += pw + 4;
        }

        // 2. 시세 입력 버튼 (상단 중앙)
        addDrawableChild(ButtonWidget.builder(Text.literal("시세 입력"), btn ->
                client.setScreen(new PriceScreen(this, job))).dimensions(x0 + 175, 11, 56, 15).build());

        // 3. 직업 탭 버튼 (광부 / 농부 / 어부 / 나무꾼 / 사냥꾼)
        Job[] tabs = Job.tabs();
        int jx = x0 + 175;
        int jw = (x0 + w - 12 - jx - (tabs.length - 1) * 4) / tabs.length;
        for (Job j : tabs) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(j.label), btn -> {
                job = j;
                lastJob = j;
                scroll = 0;
                clearAndInit();
            }).dimensions(jx, 32, jw, 16).build();
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

        // 전체 화면 배경 딤
        ctx.fill(0, 0, width, height, C_OVERLAY_BG);

        // 살목지 일러스트 30% 투명도
