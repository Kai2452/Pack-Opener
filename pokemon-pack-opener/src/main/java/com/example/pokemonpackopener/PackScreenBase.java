package com.example.pokemonpackopener;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Shared immediate-mode UI plumbing: background, panels, buttons and click hit-testing. */
abstract class PackScreenBase extends Screen {
    protected record HitBox(String id, int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    protected final List<HitBox> hits = new ArrayList<>();
    protected int age;

    protected PackScreenBase(Component title) {
        super(title);
    }

    protected abstract void onHit(String id);

    protected boolean onEmptyClick() {
        return false;
    }

    @Override
    public void tick() {
        age++;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (HitBox h : hits) {
                if (h.contains(event.x(), event.y())) {
                    Sfx.click();
                    onHit(h.id());
                    return true;
                }
            }
            if (onEmptyClick()) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    protected void drawBackground(GuiGraphicsExtractor g, float time) {
        int bands = 24;
        int top = 0xFF0B1030, bot = 0xFF2A1458;
        for (int i = 0; i < bands; i++) {
            int y0 = -8 + (height + 16) * i / bands;
            int y1 = -8 + (height + 16) * (i + 1) / bands;
            g.fill(-8, y0, width + 8, y1, Gfx.lerp(top, bot, i / (float) (bands - 1)));
        }
        Gfx.ball(g, width / 2, height / 2, Math.min(width, height) / 2 - 10, 14);
        for (int k = 0; k < 36; k++) {
            int x = (k * 97 + 13) % Math.max(1, width);
            int y = (k * 57 + 29) % Math.max(1, height);
            float tw = 0.5f + 0.5f * (float) Math.sin(time * 0.08f + k);
            int s = 1 + (k & 1);
            g.fill(x, y, x + s, y + s, Gfx.argb((int) (40 + 120 * tw), 0xFFFFFF));
        }
    }

    protected void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int border) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        g.fill(x, y, x + w, y + h, 0xE00C1030);
        g.fill(x, y, x + w, y + 1, Gfx.argb(60, 0xFFFFFF));
    }

    protected void button(GuiGraphicsExtractor g, String id, String label, int x, int y, int w, int h,
                          int color, int mx, int my, boolean enabled) {
        boolean hov = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        int base = enabled ? color : 0xFF3A3F4B;
        int top = Gfx.lighten(base, hov ? 0.38f : 0.18f);
        int bot = hov ? Gfx.lighten(base, 0.12f) : base;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 2, 0xFF05060C);
        g.fill(x, y, x + w, y + h / 2, top);
        g.fill(x, y + h / 2, x + w, y + h, bot);
        g.fill(x, y + h, x + w, y + h + 1, Gfx.darken(base, 0.45f));
        int tw = Math.max(1, font.width(label));
        float s = Math.min(1f, (w - 6f) / tw);
        Gfx.centered(g, font, label, x + w / 2f, y + (h - 8f * s) / 2f, s,
                enabled ? 0xFFFFFFFF : 0xFF8890A0, true);
        if (enabled) {
            hits.add(new HitBox(id, x, y, w, h));
        }
    }
}
