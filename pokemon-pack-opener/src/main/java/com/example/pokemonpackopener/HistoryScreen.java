package com.example.pokemonpackopener;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Inventory (unsold rewards) and history (last 1000 pulls) with scrolling. */
public class HistoryScreen extends PackScreenBase {
    private static final int ROW_H = 22;
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final Screen parent;
    private boolean showHistory;
    private int scroll;
    private int viewTop, viewBottom, viewLeft, viewRight;
    private int listSize;
    private String toast = "";
    private int toastTicks;

    public HistoryScreen(Screen parent) {
        super(Component.literal("Inventory & History"));
        this.parent = parent;
    }

    @Override
    public void tick() {
        super.tick();
        if (toastTicks > 0) toastTicks--;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    protected void onHit(String id) {
        switch (id) {
            case "tabInv" -> {
                showHistory = false;
                scroll = 0;
            }
            case "tabHist" -> {
                showHistory = true;
                scroll = 0;
            }
            case "sell" -> {
                PackStore s = PackStore.get();
                int n = s.inventory.size();
                if (n > 0) {
                    long v = s.sellAll();
                    Sfx.sell();
                    toast = "Sold " + n + " Pok\u00e9mon for " + Gfx.money(v) + " (simulated)";
                    toastTicks = 110;
                    scroll = 0;
                }
            }
            case "back" -> onClose();
            default -> { }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= (int) (scrollY * ROW_H);
        clampScroll();
        return true;
    }

    private void clampScroll() {
        int max = Math.max(0, listSize * ROW_H - (viewBottom - viewTop));
        scroll = Math.max(0, Math.min(max, scroll));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        hits.clear();
        float time = age + pt;
        PackStore store = PackStore.get();
        List<PackStore.Entry> list = showHistory ? store.history : store.inventory;
        listSize = list.size();

        drawBackground(g, time);
        Gfx.centered(g, font, "INVENTORY & HISTORY", width / 2f, 8, 2f, 0xFFFFCB05, true);

        // tabs
        int tw = 110;
        int tx = width / 2 - tw - 3;
        button(g, "tabInv", "Inventory (" + store.inventory.size() + ")", tx, 30, tw, 16,
                showHistory ? 0xFF3C4260 : 0xFF5A3FA0, mx, my, true);
        button(g, "tabHist", "History (" + store.history.size() + ")", tx + tw + 6, 30, tw, 16,
                showHistory ? 0xFF5A3FA0 : 0xFF3C4260, mx, my, true);

        String summary = showHistory
                ? "Total value of everything you've opened: " + Gfx.money(store.lifetimeValue)
                : "Inventory value: " + Gfx.money(store.inventoryValue()) + "   |   Sold so far (simulated): " + Gfx.money(store.soldValue);
        Gfx.centered(g, font, summary, width / 2f, 54, 1f, 0xFF7CFFB0, true);

        viewLeft = Math.max(10, width / 2 - 200);
        viewRight = Math.min(width - 10, width / 2 + 200);
        viewTop = 68;
        viewBottom = height - 44;
        clampScroll();

        panel(g, viewLeft - 2, viewTop - 2, viewRight - viewLeft + 4, viewBottom - viewTop + 4, 0xFFFFCB05);

        if (list.isEmpty()) {
            Gfx.centered(g, font, showHistory ? "No packs opened yet." : "Inventory is empty \u2014 open some packs!",
                    width / 2f, viewTop + 20, 1f, 0xFF9AA4C8, false);
        } else {
            g.enableScissor(viewLeft, viewTop, viewRight, viewBottom);
            int first = Math.max(0, scroll / ROW_H);
            int last = Math.min(listSize - 1, (scroll + (viewBottom - viewTop)) / ROW_H);
            for (int i = first; i <= last; i++) {
                PackStore.Entry e = list.get(listSize - 1 - i); // newest first
                int y = viewTop + i * ROW_H - scroll;
                g.fill(viewLeft, y, viewRight, y + ROW_H, (i & 1) == 0 ? 0x30FFFFFF : 0x14FFFFFF);
                Rarity r = rarityOf(e.rarity);
                int col = r == Rarity.MYTHICAL ? Gfx.rainbow(time, i * 0.05f) : r.color;
                g.fill(viewLeft, y, viewLeft + 2, y + ROW_H, col);
                Pokemon m = PokemonPool.byName(e.name);
                if (m != null) Gfx.icon(g, m, viewLeft + 8, y + 3, 2);
                g.text(font, e.name, viewLeft + 32, y + 3, 0xFFFFFFFF, true);
                g.text(font, r.label, viewLeft + 32, y + 12, col, false);
                if (showHistory) {
                    g.text(font, TIME_FMT.format(Instant.ofEpochMilli(e.time)), viewLeft + 120, y + 7, 0xFF8E98B8, false);
                }
                String money = Gfx.money(e.value);
                g.text(font, money, viewRight - 8 - font.width(money), y + 7, 0xFF7CFFB0, true);
            }
            g.disableScissor();
        }

        int by = height - 32;
        int w = 110;
        int x0 = width / 2 - (w * 2 + 8) / 2;
        button(g, "sell", showHistory ? "Sell All (inventory tab)" : "Sell All  " + Gfx.compact(store.inventoryValue()),
                x0 - 20, by, w + 20, 18, 0xFF1E9E5A, mx, my, !showHistory && !store.inventory.isEmpty());
        button(g, "back", "Back", x0 + w + 8, by, w - 20, 18, 0xFF3C4260, mx, my, true);

        if (toastTicks > 0) {
            Gfx.centered(g, font, toast, width / 2f, height - 46, 1f, Gfx.argb(Math.min(255, toastTicks * 10), 0x7CFFB0), true);
        }
    }

    private static Rarity rarityOf(String s) {
        try {
            return Rarity.valueOf(s);
        } catch (Exception e) {
            return Rarity.COMMON;
        }
    }
}
