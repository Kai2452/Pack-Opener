package com.example.pokemonpackopener;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

/** Small drawing helpers (all built on fill() + pose transforms, so very cheap). */
final class Gfx {
    private Gfx() {}

    static int argb(int a, int rgb) {
        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static int lighten(int c, float t) {
        return lerp(c, 0xFFFFFFFF, t);
    }

    static int darken(int c, float t) {
        return lerp(c, 0xFF000000, t);
    }

    static int hsv(float h, float s, float v) {
        h = ((h % 1f) + 1f) % 1f;
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    static int rainbow(float time, float offset) {
        return hsv(time * 0.012f + offset, 0.6f, 1f);
    }

    static float easeOutBack(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float c1 = 1.70158f, c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    static float easeOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - t) * (1f - t);
    }

    static String money(long v) {
        return String.format(Locale.US, "$%,d", v);
    }

    static String compact(long v) {
        if (v >= 1_000_000L) {
            double m = v / 1_000_000.0;
            return m == Math.floor(m) ? "$" + (long) m + "M" : String.format(Locale.US, "$%.1fM", m);
        }
        if (v >= 1_000L) {
            double k = v / 1_000.0;
            return k == Math.floor(k) ? "$" + (long) k + "K" : String.format(Locale.US, "$%.1fK", k);
        }
        return "$" + v;
    }

    static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int th, int c) {
        g.fill(x, y, x + w, y + th, c);
        g.fill(x, y + h - th, x + w, y + h, c);
        g.fill(x, y + th, x + th, y + h - th, c);
        g.fill(x + w - th, y + th, x + w, y + h - th, c);
    }

    static void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            g.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, color);
        }
    }

    static void ball(GuiGraphicsExtractor g, int cx, int cy, int r, int a) {
        int band = Math.max(1, r / 8);
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            int rgb = Math.abs(dy) <= band ? 0x202028 : (dy < 0 ? 0xE63946 : 0xF4F4F8);
            g.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, argb(a, rgb));
        }
        int br = Math.max(2, r / 4);
        disc(g, cx, cy, br + Math.max(1, r / 16), argb(a, 0x202028));
        disc(g, cx, cy, br, argb(a, 0xF4F4F8));
    }

    static void sparkle(GuiGraphicsExtractor g, int x, int y, int s, int color) {
        g.fill(x - s, y, x + s + 1, y + 1, color);
        g.fill(x, y - s, x + 1, y + s + 1, color);
    }

    /** Draws text centred on cx, scaled. */
    static void centered(GuiGraphicsExtractor g, Font font, String s, float cx, float y, float scale, int color, boolean shadow) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, y);
        pose.scale(scale, scale);
        g.text(font, s, -font.width(s) / 2, 0, color, shadow);
        pose.popMatrix();
    }

    /** Like centered() but shrinks the text if it would be wider than maxW. */
    static void fit(GuiGraphicsExtractor g, Font font, String s, float cx, float y, float baseScale, float maxW, int color, boolean shadow) {
        float w = font.width(s) * baseScale;
        float sc = w > maxW ? baseScale * (maxW / w) : baseScale;
        centered(g, font, s, cx, y, sc, color, shadow);
    }

    /** Draws a Pokémon's unique 8x8 badge icon. */
    static void icon(GuiGraphicsExtractor g, Pokemon m, int x, int y, int cell) {
        int c1 = m.t1.color;
        int c2 = m.t2 == null ? darken(m.t1.color, 0.3f) : m.t2.color;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (m.grid[row * 8 + col] != 0) {
                    int px = x + col * cell, py = y + row * cell;
                    g.fill(px - 1, py - 1, px + cell + 1, py + cell + 1, 0xFF10101A);
                }
            }
        }
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int v = m.grid[row * 8 + col];
                if (v == 0) continue;
                int px = x + col * cell, py = y + row * cell;
                switch (v) {
                    case 1 -> g.fill(px, py, px + cell, py + cell, c1);
                    case 2 -> g.fill(px, py, px + cell, py + cell, c2);
                    default -> {
                        g.fill(px, py, px + cell, py + cell, 0xFFFFFFFF);
                        int in = Math.max(1, cell / 3);
                        g.fill(px + cell - in - (cell > 3 ? 1 : 0), py + (cell - in) / 2, px + cell - (cell > 3 ? 1 : 0), py + (cell - in) / 2 + in, 0xFF101018);
                    }
                }
            }
        }
    }
}
