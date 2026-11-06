package com.lifestats.screen;

import com.lifestats.data.ItemRegistry;
import com.lifestats.data.PriceManager;
import com.lifestats.model.ItemDef;
import com.lifestats.model.Job;
import com.lifestats.track.InventoryTracker;
import com.lifestats.util.Fmt;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * 시세 입력 화면. 품목마다 [단위 버튼][가격 입력칸]이 있고, 입력값은 "선택한 단위 1개당 가격"이다.
 * 내부 저장은 항상 개당 가격(prices.json)이다.
 */
public class PriceScreen extends Screen {
    private static final int C_PANEL = 0xFF0E2433;
    private static final int C_BORDER = 0xFF1F8FB5;
    private static final int C_NAME = 0xFFE6EEF2;
    private static final int C_DIM = 0xFF8FA3AE;
    private static final int C_TITLE = 0xFFFFFFFF;

    private record Cell(ItemDef def, int x, int y, int nameMax) {
    }

    private final Screen parent;
    private Job job;
    private int page;
    private int pages = 1;
    private final List<Cell> cells = new ArrayList<>();

    public PriceScreen(Screen parent, Job job) {
        super(Text.literal("시세 입력"));
        this.parent = parent;
        this.job = job;
    }

    private List<ItemDef> items() {
        List<ItemDef> list = new ArrayList<>(ItemRegistry.items(job));
        list.addAll(ItemRegistry.items(Job.COMMON));
        return list;
    }

    @Override
    protected void init() {
        cells.clear();
        List<ItemDef> items = items();

        int colW = Math.min(300, (width - 24) / 2);
        int x0 = (width - (colW * 2 + 8)) / 2;
        int top = 60;
        int rowH = 22;
        int rows = Math.max(3, (height - top - 40) / rowH);
        int perPage = rows * 2;
        pages = Math.max(1, (items.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));

        // 직업 탭
        Job[] tabs = Job.tabs();
        int tw = Math.min(70, (colW * 2 + 8 - (tabs.length - 1) * 4) / tabs.length);
        int jx = x0;
        for (Job j : tabs) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(j.label), btn -> {
                job = j;
                page = 0;
                clearAndInit();
            }).dimensions(jx, 24, tw, 18).build();
            b.active = j != job;
            addDrawableChild(b);
            jx += tw + 4;
        }

        int unitW = 46;
        int fieldW = Math.max(50, Math.min(84, colW - unitW - 70));
        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= items.size()) break;
            ItemDef def = items.get(idx);
            int col = i / rows;
            int row = i % rows;
            int cx = x0 + col * (colW + 8);
            int cy = top + row * rowH;
            int fieldX = cx + colW - fieldW;
            int unitX = fieldX - unitW - 4;

            TextFieldWidget field = new TextFieldWidget(textRenderer, fieldX, cy, fieldW, 18, Text.literal(def.name()));
            field.setTextPredicate(s -> s.matches("[0-9.]*"));
            field.setMaxLength(14);
            field.setText(Fmt.plain(PriceManager.perPiece(def.name()) * PriceManager.unit(def.name())));
            field.setChangedListener(s -> {
                double v;
                try {
                    v = s.isEmpty() ? 0 : Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    return;
                }
                PriceManager.setPerPiece(def.name(), v / PriceManager.unit(def.name()));
            });
            addDrawableChild(field);

            addDrawableChild(ButtonWidget.builder(Text.literal(Fmt.unitLabel(PriceManager.unit(def.name()))), btn -> {
                int next = Fmt.nextUnit(PriceManager.unit(def.name()));
                PriceManager.setUnit(def.name(), next);
                btn.setMessage(Text.literal(Fmt.unitLabel(next)));
                field.setText(Fmt.plain(PriceManager.perPiece(def.name()) * next));
            }).dimensions(unitX, cy, unitW, 18).build());

            cells.add(new Cell(def, cx, cy, unitX - cx - 22));
        }

        int by = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), btn -> {
            page = Math.max(0, page - 1);
            clearAndInit();
        }).dimensions(width / 2 - 70, by, 24, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), btn -> {
            page = Math.min(pages - 1, page + 1);
            clearAndInit();
        }).dimensions(width / 2 - 42, by, 24, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("완료"), btn -> close())
                .dimensions(width / 2 + 10, by, 60, 18).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 직접 그린다
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xC0000000);
        int colW = Math.min(300, (width - 24) / 2);
        int x0 = (width - (colW * 2 + 8)) / 2;
        ctx.fill(x0 - 8, 8, x0 + colW * 2 + 16, height - 8, C_PANEL);
        ctx.drawBorder(x0 - 8, 8, colW * 2 + 24, height - 16, C_BORDER);

        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawText(textRenderer, "시세 입력", x0, 12, C_TITLE, true);
        String hint = "단위를 누르면 1개 → 1세트 → 1설커 순으로 바뀝니다";
        ctx.drawText(textRenderer, hint, x0 + colW * 2 + 8 - textRenderer.getWidth(hint), 12, C_DIM, false);
        ctx.drawText(textRenderer, "품목", x0, 48, C_DIM, false);

        for (Cell c : cells) {
            ItemStack icon = InventoryTracker.ICONS.get(c.def().name());
            if (icon != null) ctx.drawItem(icon, c.x(), c.y() + 1);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(c.def().name(), Math.max(20, c.nameMax())),
                    c.x() + 20, c.y() + 5, C_NAME, false);
        }

        if (cells.isEmpty()) {
            ctx.drawText(textRenderer, job.label + " 품목이 아직 없습니다 (config/lifestats/jobs.json)", x0, 70, C_DIM, false);
        }
        String pg = (page + 1) + " / " + pages;
        ctx.drawText(textRenderer, pg, width / 2 - 70 - 8 - textRenderer.getWidth(pg), height - 23, C_DIM, false);
    }

    @Override
    public void close() {
        PriceManager.save();
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
