package com.example.pokemonpackopener;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class PackOpenerScreen extends PackScreenBase {
    private enum Phase { IDLE, SHAKE, REVEAL, SUMMARY }

    private static final int SHAKE_TICKS = 38;
    private static final int GOLD = 0xFFFFCB05;

    private Phase phase = Phase.IDLE;
    private int phaseTick;
    private int lastCount = 1;
    private List<Pull> pulls = List.of();
    private int[] revealAt = new int[0];
    private int[] hold = new int[0];
    private int revealed;
    private int endTick;
    private long openValue;
    private long runningValue;
    private String openValueText = "$0";
    private String runningText = "$0";
    private Rarity bestRarity = Rarity.COMMON;
    private float flash;
    private int shakeTicks;
    private String toast = "";
    private int toastTicks;
    private final Random rng = new Random();

    private final String[] oddsA = new String[6];
    private final String[] oddsB = new String[6];
    private final String[] stats = new String[5];
    private String sellLabel = "Sell All";
    private boolean inventoryEmpty = true;

    public PackOpenerScreen() {
        super(Component.literal("Pok\u00e9 Pack Opener"));
    }

    @Override
    protected void init() {
        refreshStrings();
    }

    private void refreshStrings() {
        PackConfig c = PackConfig.get();
        PackStore s = PackStore.get();
        double total = c.totalWeight();
        Rarity[] rs = Rarity.values();
        for (int i = 0; i < rs.length; i++) {
            PackConfig.RarityCfg rc = c.cfg(rs[i]);
            oddsA[i] = String.format(Locale.US, "%s  %.1f%%", rs[i].label, 100.0 * rc.weight / total);
            oddsB[i] = Gfx.compact(Math.round(rc.min * c.priceMultiplier)) + " - "
                    + Gfx.compact(Math.round(rc.max * c.priceMultiplier));
        }
        stats[0] = "Packs opened: " + String.format(Locale.US, "%,d", s.packsOpened);
        stats[1] = "Total value opened:";
        stats[2] = Gfx.money(s.lifetimeValue);
        stats[3] = "In inventory: " + s.inventory.size() + " (" + Gfx.compact(s.inventoryValue()) + ")";
        stats[4] = s.bestValue > 0 ? "Best: " + s.bestName + " " + Gfx.compact(s.bestValue) : "Best pull: -";
        inventoryEmpty = s.inventory.isEmpty();
        sellLabel = inventoryEmpty ? "Sell All" : "Sell All  " + Gfx.money(s.inventoryValue());
    }

    // ------------------------------------------------------------------ logic

    private void startOpen(int n) {
        PackConfig cfg = PackConfig.get();
        lastCount = n;
        List<Pull> list = new ArrayList<>(n);
        List<PackStore.Entry> entries = new ArrayList<>(n);
        long now = System.currentTimeMillis();
        long total = 0;
        bestRarity = Rarity.COMMON;
        for (int i = 0; i < n; i++) {
            Pull p = PackRoller.roll(cfg, rng);
            list.add(p);
            total += p.value();
            entries.add(PackStore.entry(p.mon().name, p.mon().rarity.name(), p.value(), now));
            if (p.mon().rarity.ordinal() > bestRarity.ordinal()) bestRarity = p.mon().rarity;
        }
        PackStore.get().recordPulls(entries);

        pulls = list;
        openValue = total;
        openValueText = Gfx.money(total);
        runningValue = 0;
        runningText = "$0";

        int base = n >= 10 ? 7 : (n >= 5 ? 10 : 16);
        revealAt = new int[n];
        hold = new int[n];
        int t = 8;
        for (int i = 0; i < n; i++) {
            revealAt[i] = t;
            Rarity r = list.get(i).mon().rarity;
            int extra = switch (r) {
                case EPIC -> 6;
                case LEGENDARY -> 58;
                case MYTHICAL -> 84;
                default -> 0;
            };
            hold[i] = base + extra;
            t += hold[i];
        }
        endTick = t + 14;

        revealed = 0;
        phase = Phase.SHAKE;
        phaseTick = 0;
        flash = 0f;
        Sfx.tear();
        refreshStrings();
    }

    private void skip() {
        if (phase == Phase.SHAKE || phase == Phase.REVEAL) {
            if (phase == Phase.SHAKE) Sfx.tear();
            runningValue = openValue;
            runningText = openValueText;
            revealed = pulls.size();
            phase = Phase.SUMMARY;
            phaseTick = 0;
            flash = 0f;
            shakeTicks = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        phaseTick++;
        if (toastTicks > 0) toastTicks--;
        if (shakeTicks > 0) shakeTicks--;
        if (flash > 0f) flash = Math.max(0f, flash - 0.06f);

        switch (phase) {
            case SHAKE -> {
                if (phaseTick % 5 == 0) Sfx.rattle(phaseTick / (float) SHAKE_TICKS);
                if (phaseTick >= SHAKE_TICKS) {
                    phase = Phase.REVEAL;
                    phaseTick = 0;
                    flash = 0.85f;
                    Sfx.burst();
                }
            }
            case REVEAL -> {
                while (revealed < pulls.size() && phaseTick >= revealAt[revealed]) {
                    onReveal(revealed);
                    revealed++;
                }
                if (revealed >= pulls.size() && phaseTick >= endTick) {
                    phase = Phase.SUMMARY;
                    phaseTick = 0;
                }
            }
            default -> { }
        }
    }

    private void onReveal(int i) {
        Pull p = pulls.get(i);
        Rarity r = p.mon().rarity;
        runningValue += p.value();
        runningText = Gfx.money(runningValue);
        Sfx.reveal(r);
        if (r.isCinematic()) {
            flash = r == Rarity.MYTHICAL ? 0.9f : 0.6f;
            shakeTicks = r == Rarity.MYTHICAL ? 22 : 14;
        }
    }

    @Override
    protected void onHit(String id) {
        switch (id) {
            case "open1" -> startOpen(1);
            case "open5" -> startOpen(5);
            case "open10" -> startOpen(10);
            case "again" -> startOpen(lastCount);
            case "back" -> {
                phase = Phase.IDLE;
                phaseTick = 0;
                refreshStrings();
            }
            case "history" -> minecraft.setScreen(new HistoryScreen(this));
            case "sell" -> sellAll();
            case "reload" -> {
                PackConfig.load();
                refreshStrings();
                toast("Config reloaded");
            }
            default -> { }
        }
    }

    private void sellAll() {
        PackStore s = PackStore.get();
        int n = s.inventory.size();
        if (n == 0) return;
        long v = s.sellAll();
        Sfx.sell();
        refreshStrings();
        toast("Sold " + n + " Pok\u00e9mon for " + Gfx.money(v) + "  (simulated - not real money)");
    }

    private void toast(String s) {
        toast = s;
        toastTicks = 110;
    }

    @Override
    protected boolean onEmptyClick() {
        if (phase == Phase.SHAKE || phase == Phase.REVEAL) {
            skip();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        if ((k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_ENTER) && (phase == Phase.SHAKE || phase == Phase.REVEAL)) {
            skip();
            return true;
        }
        return super.keyPressed(event);
    }

    // ---------------------------------------------------------------- render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        hits.clear();
        float time = age + pt;
        PackConfig cfg = PackConfig.get();
        var pose = g.pose();
        pose.pushMatrix();
        if (shakeTicks > 0 && cfg.screenShake) {
            float k = shakeTicks / 14f;
            pose.translate((float) Math.sin(time * 2.9f) * k * 4f, (float) Math.cos(time * 3.7f) * k * 4f);
        }
        drawBackground(g, time);
        switch (phase) {
            case IDLE -> renderIdle(g, mx, my, time);
            case SHAKE -> renderShake(g, time, pt);
            case REVEAL -> renderReveal(g, time, pt);
            case SUMMARY -> renderSummary(g, mx, my, time, pt);
        }
        if (toastTicks > 0) {
            int a = Math.min(255, toastTicks * 10);
            Gfx.centered(g, font, toast, width / 2f, height - 80, 1f, Gfx.argb(a, 0x7CFFB0), true);
        }
        pose.popMatrix();
        if (flash > 0f && cfg.flashEffects) {
            g.fill(0, 0, width, height, Gfx.argb((int) (flash * 255), 0xFFFFFF));
        }
    }

    private void title(GuiGraphicsExtractor g, String main, String sub) {
        Gfx.centered(g, font, main, width / 2f, 8, 2f, GOLD, true);
        if (sub != null) Gfx.centered(g, font, sub, width / 2f, 28, 1f, 0xFF9AA4C8, false);
    }

    private void renderIdle(GuiGraphicsExtractor g, int mx, int my, float time) {
        title(g, "POK\u00c9 PACK OPENER", "Client-side reward simulator \u2014 values are NOT real DonutSMP money");
        int panelW = Math.max(140, Math.min(190, width / 4));
        int top = 46;

        // --- Odds panel (shown before every open)
        int rowH = 24;
        int oh = 20 + rowH * 6 + 4;
        panel(g, 10, top, panelW, oh, GOLD);
        g.text(font, "ODDS PER PACK", 18, top + 6, GOLD, true);
        Rarity[] rs = Rarity.values();
        for (int i = 0; i < rs.length; i++) {
            int y = top + 20 + i * rowH;
            int col = rs[i] == Rarity.MYTHICAL ? Gfx.rainbow(time, 0f) : rs[i].color;
            g.fill(16, y + 1, 20, y + 17, col);
            g.text(font, oddsA[i], 25, y, col, true);
            g.text(font, oddsB[i], 25, y + 10, 0xFF8E98B8, false);
        }

        // --- Stats panel
        int sx = width - panelW - 10;
        panel(g, sx, top, panelW, 120, GOLD);
        g.text(font, "YOUR COLLECTION", sx + 8, top + 6, GOLD, true);
        g.text(font, stats[0], sx + 8, top + 22, 0xFFFFFFFF, false);
        g.text(font, stats[1], sx + 8, top + 36, 0xFF9AA4C8, false);
        Gfx.fit(g, font, stats[2], sx + panelW / 2f, top + 47, 1.4f, panelW - 14, 0xFF7CFFB0, true);
        g.text(font, stats[3], sx + 8, top + 70, 0xFFFFFFFF, false);
        g.text(font, stats[4], sx + 8, top + 84, 0xFFFFC21A, false);
        g.text(font, "Sold (sim): " + Gfx.compact(PackStore.get().soldValue), sx + 8, top + 98, 0xFF9AA4C8, false);

        // --- Pack + open buttons
        int centerW = width - 2 * (panelW + 24);
        int ph = Math.max(56, Math.min(110, height - 250));
        int pw = ph * 2 / 3 + 8;
        int pcx = width / 2;
        int pcy = top + ph / 2 + 6;
        drawPack(g, pcx, pcy, pw, ph, time, 0f);
        int by = top + ph + 22;
        int bw = Math.max(44, Math.min(62, (centerW - 12) / 3));
        int bx = pcx - (bw * 3 + 12) / 2;
        button(g, "open1", "Open 1x", bx, by, bw, 18, 0xFF2E7DD7, mx, my, true);
        button(g, "open5", "Open 5x", bx + bw + 6, by, bw, 18, 0xFFB8741A, mx, my, true);
        button(g, "open10", "Open 10x", bx + 2 * (bw + 6), by, bw, 18, 0xFFC0392B, mx, my, true);

        // --- Bottom row
        int rowY = height - 50;
        int w1 = 120, w2 = 170, w3 = 90, gap = 8;
        int x0 = pcx - (w1 + w2 + w3 + 2 * gap) / 2;
        button(g, "history", "Inventory & History", x0, rowY, w1, 18, 0xFF5A3FA0, mx, my, true);
        button(g, "sell", sellLabel, x0 + w1 + gap, rowY, w2, 18, 0xFF1E9E5A, mx, my, !inventoryEmpty);
        button(g, "reload", "Reload Config", x0 + w1 + w2 + 2 * gap, rowY, w3, 18, 0xFF3C4260, mx, my, true);
        Gfx.centered(g, font, "Simulation only \u2014 nothing here is deposited to your DonutSMP balance. "
                + "Edit prices in config/pokemon_pack_opener.json", width / 2f, height - 18, 0.8f, 0xFF7480A8, false);
    }

    private void renderShake(GuiGraphicsExtractor g, float time, float pt) {
        float prog = Math.min(1f, (phaseTick + pt) / SHAKE_TICKS);
        title(g, lastCount == 1 ? "OPENING PACK..." : "OPENING " + lastCount + " PACKS...", "Click or press SPACE to skip");
        int cx = width / 2;
        int ph = Math.max(80, Math.min(150, height / 2));
        int pw = ph * 2 / 3 + 10;
        int cy = height / 2 + 4;

        int aura = bestRarity == Rarity.MYTHICAL ? Gfx.rainbow(time, 0f) : bestRarity.color;
        if (prog > 0.25f) {
            float k = (prog - 0.25f) / 0.75f;
            for (int i = 0; i < 4; i++) {
                Gfx.disc(g, cx, cy, (int) (ph * (0.45f + 0.5f * k) * (1f + i * 0.22f)), Gfx.argb((int) (22 * k), aura));
            }
        }
        float amp = 1f + prog * prog * 8f;
        drawPack(g, cx, cy, pw, ph, time * 3f, amp);
        if (prog > 0.55f) {
            float k = (prog - 0.55f) / 0.45f;
            int lw = 1 + (int) (k * 3);
            g.fill(cx - lw, cy - ph / 2, cx + lw, cy + ph / 2, Gfx.argb((int) (80 + 175 * k), 0xFFFFFF));
        }
    }

    private void renderReveal(GuiGraphicsExtractor g, float time, float pt) {
        title(g, "PACK OPENING", null);
        Gfx.centered(g, font, "Value so far: " + runningText, width / 2f, 30, 1.2f, 0xFF7CFFB0, true);
        Gfx.centered(g, font, "Click or press SPACE to skip", width / 2f, height - 14, 0.8f, 0xFF7480A8, false);
        renderCards(g, time, pt);

        int last = revealed - 1;
        if (last >= 0 && pulls.get(last).mon().rarity.isCinematic()) {
            float a = phaseTick - revealAt[last] + pt;
            if (a < hold[last]) {
                drawCinematic(g, pulls.get(last), a, hold[last], time);
            }
        }
    }

    private void renderSummary(GuiGraphicsExtractor g, int mx, int my, float time, float pt) {
        title(g, "PACK RESULTS", null);
        Gfx.centered(g, font, "PACK VALUE", width / 2f, 29, 1f, 0xFF9AA4C8, false);
        Gfx.centered(g, font, openValueText, width / 2f, 39, 1.8f, 0xFF7CFFB0, true);
        renderCards(g, time, pt);

        int rowY = height - 36;
        int w1 = 130, w2 = 130, w3 = 80, gap = 8;
        int x0 = width / 2 - (w1 + w2 + w3 + 2 * gap) / 2;
        button(g, "again", "Open Again (" + lastCount + "x)", x0, rowY, w1, 18, 0xFFC0392B, mx, my, true);
        button(g, "history", "Inventory & History", x0 + w1 + gap, rowY, w2, 18, 0xFF5A3FA0, mx, my, true);
        button(g, "back", "Back", x0 + w1 + w2 + 2 * gap, rowY, w3, 18, 0xFF3C4260, mx, my, true);
    }

    // ------------------------------------------------------------------ cards

    private void renderCards(GuiGraphicsExtractor g, float time, float pt) {
        int n = pulls.size();
        if (n == 0) return;
        int cols = n <= 5 ? n : 5;
        int rows = (n + cols - 1) / cols;
        int gap = 8;
        int availH = height - 100;
        int ch = Math.max(40, Math.min(110, availH / rows - gap));
        int cw = ch * 20 / 29;
        int maxCw = (width - 24) / cols - gap;
        if (cw > maxCw) {
            cw = Math.max(24, maxCw);
            ch = cw * 29 / 20;
        }
        int gridH = rows * (ch + gap) - gap;
        int startY = 56 + Math.max(0, (availH - gridH) / 2);

        for (int i = 0; i < n; i++) {
            int row = i / cols, col = i % cols;
            int inRow = Math.min(cols, n - row * cols);
            int rowW = inRow * (cw + gap) - gap;
            int x = (width - rowW) / 2 + col * (cw + gap);
            int y = startY + row * (ch + gap);

            float flip;
            float ageI = 99f;
            if (phase == Phase.SUMMARY) {
                flip = 1f;
            } else if (i < revealed) {
                ageI = phaseTick - revealAt[i] + pt;
                flip = Math.min(1f, ageI / 8f);
            } else {
                flip = 0f;
            }
            Pull p = pulls.get(i);
            if (flip >= 1f && ageI < 14f && p.mon().rarity.ordinal() >= Rarity.RARE.ordinal()) {
                int e = (int) (ageI * 1.6f) + 2;
                int col2 = p.mon().rarity == Rarity.MYTHICAL ? Gfx.rainbow(time, i * 0.07f) : p.mon().rarity.color;
                Gfx.outline(g, x - e, y - e, cw + 2 * e, ch + 2 * e, 1, Gfx.argb((int) (200 * (1f - ageI / 14f)), col2));
            }
            drawCard(g, p, x, y, cw, ch, flip, time, i);
        }
    }

    private void drawCardBack(GuiGraphicsExtractor g, int x, int y, int w, int h, float time, int seed) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF05060C);
        g.fill(x, y, x + w, y + h, 0xFF1B2A7A);
        g.fill(x, y, x + w, y + h / 3, 0xFF243499);
        Gfx.outline(g, x, y, w, h, 2, GOLD);
        if (w >= 20) {
            Gfx.ball(g, x + w / 2, y + h / 2, Math.max(4, Math.min(w, h) / 4), 255);
        }
        int sx = x + 2 + (int) ((time * 1.5f + seed * 9) % Math.max(1, w - 4));
        g.fill(sx, y + 2, Math.min(x + w - 2, sx + 2), y + h - 2, Gfx.argb(40, 0xFFFFFF));
    }

    private void drawCard(GuiGraphicsExtractor g, Pull p, int x, int y, int w, int h, float flip, float time, int seed) {
        float f = Math.abs(flip * 2f - 1f);
        int ew = Math.max(2, (int) (w * f));
        int ex = x + (w - ew) / 2;
        if (flip < 0.5f) {
            drawCardBack(g, ex, y, ew, h, time, seed);
            return;
        }
        Rarity r = p.mon().rarity;
        int col = r == Rarity.MYTHICAL ? Gfx.rainbow(time, seed * 0.07f) : r.color;
        boolean full = ew >= w;

        if (full) {
            float pulse = 0.65f + 0.35f * (float) Math.sin(time * 0.18f + seed);
            int layers = switch (r) {
                case RARE -> 2;
                case EPIC -> 3;
                case LEGENDARY -> 6;
                case MYTHICAL -> 7;
                default -> 0;
            };
            int baseA = r == Rarity.RARE ? 14 : (r == Rarity.EPIC ? 18 : 26);
            for (int i = layers; i >= 1; i--) {
                int e = i * 2;
                g.fill(ex - e, y - e, ex + ew + e, y + h + e, Gfx.argb((int) (baseA * pulse), col));
            }
        }
        g.fill(ex - 1, y - 1, ex + ew + 1, y + h + 1, 0xFF05060C);
        g.fill(ex, y, ex + ew, y + h / 2, Gfx.lerp(0xFF1C2142, col, 0.20f));
        g.fill(ex, y + h / 2, ex + ew, y + h, 0xFF11142B);
        Gfx.outline(g, ex, y, ew, h, 2, col);
        if (!full) return;

        float sc = Math.max(1f, w / 48f);
        float cx = ex + ew / 2f;
        g.fill(ex + 2, y + 2, ex + ew - 2, y + 3 + (int) (7 * Math.min(sc, 1.6f)), Gfx.argb(90, 0x000000));
        Gfx.fit(g, font, r.label.toUpperCase(Locale.ROOT), cx, y + 3, 0.7f * Math.min(sc, 1.6f), ew - 6, col, true);

        int cell = Math.max(2, Math.min((w - 10) / 8, (int) ((h - 39 * Math.min(sc, 1.6f)) / 8)));
        int iconY = y + (int) (13 * Math.min(sc, 1.6f)) + Math.max(0, (h - (int) (39 * Math.min(sc, 1.6f)) - 8 * cell) / 2);
        Gfx.icon(g, p.mon(), ex + (ew - 8 * cell) / 2, iconY, cell);
        Gfx.fit(g, font, p.mon().name, cx, iconY + 8 * cell + 4, Math.min(sc, 1.6f), ew - 6, 0xFFFFFFFF, true);
        Gfx.fit(g, font, p.moneyText(), cx, y + h - 4 - (int) (8 * Math.min(sc, 1.6f)), Math.min(sc, 1.6f), ew - 6, 0xFF7CFFB0, true);

        if (r.isCinematic()) {
            for (int k = 0; k < 4; k++) {
                double ang = time * 0.05 + k * 1.5708 + seed;
                int px = (int) (cx + Math.cos(ang) * w * 0.68);
                int py = (int) (y + h / 2f + Math.sin(ang) * h * 0.58);
                Gfx.sparkle(g, px, py, 2 + (k & 1), Gfx.argb(230, k % 2 == 0 ? 0xFFFFFF : (col & 0xFFFFFF)));
            }
        }
    }

    private void drawPack(GuiGraphicsExtractor g, int cx, int cy, int w, int h, float time, float amp) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx + (float) Math.sin(time * 2.1f) * amp, cy + (float) Math.cos(time * 2.9f) * amp * 0.4f);
        pose.rotate((float) Math.sin(time * 1.7f) * amp * 0.012f);
        int x = -w / 2, y = -h / 2;
        g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x66000000);
        g.fill(x, y, x + w, y + h, 0xFF16286E);
        g.fill(x, y, x + w, y + h / 3, 0xFF1F37A0);
        g.fill(x, y + h * 2 / 3, x + w, y + h, 0xFF101C55);
        Gfx.outline(g, x, y, w, h, 2, GOLD);
        int crimp = Math.max(4, h / 14);
        g.fill(x, y, x + w, y + crimp, 0xFFC9CDE0);
        g.fill(x, y + h - crimp, x + w, y + h, 0xFFC9CDE0);
        for (int i = 0; i < w; i += 3) {
            g.fill(x + i, y, x + i + 1, y + crimp, 0xFF8A90AD);
            g.fill(x + i, y + h - crimp, x + i + 1, y + h, 0xFF8A90AD);
        }
        float sc = w / 64f;
        Gfx.centered(g, font, "POK\u00c9", 0, y + crimp + 3 * sc, 1.4f * sc, GOLD, true);
        Gfx.ball(g, 0, 0, Math.max(6, w / 4), 255);
        Gfx.centered(g, font, "MONEY PACK", 0, y + h - crimp - 10 * sc, 0.8f * sc, 0xFFFFFFFF, true);
        // moving foil shine, clipped to the pack
        int stripe = (int) ((time * 2f) % (w * 2f)) - w / 2;
        for (int row = crimp; row < h - crimp; row++) {
            int sx = stripe + (int) (row * 0.6f);
            int x1 = Math.max(0, sx), x2 = Math.min(w - 2, sx + 8);
            if (x2 > x1) {
                g.fill(x + x1 + 1, y + row, x + x2 + 1, y + row + 1, 0x33FFFFFF);
            }
        }
        pose.popMatrix();
    }

    // --------------------------------------------------- legendary / mythical

    private void drawCinematic(GuiGraphicsExtractor g, Pull p, float a, int holdTicks, float time) {
        Rarity r = p.mon().rarity;
        boolean myth = r == Rarity.MYTHICAL;
        float fadeIn = Math.min(1f, a / 6f);
        float fadeOut = Math.min(1f, (holdTicks - a) / 10f);
        float k = Math.max(0f, Math.min(fadeIn, fadeOut));
        int rgb = (myth ? Gfx.rainbow(time, 0f) : r.color) & 0xFFFFFF;
        int cx = width / 2, cy = height / 2;

        g.fill(-8, -8, width + 8, height + 8, Gfx.argb((int) (175 * k), 0x000010));

        // rotating sunburst
        int len = (int) (Math.max(width, height) * 0.8f * Gfx.easeOut(a / 12f));
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy + 10);
        for (int i = 0; i < 14; i++) {
            pose.pushMatrix();
            pose.rotate((float) (i * (Math.PI * 2 / 14) + time * 0.02f));
            g.fill(0, -5, len, 5, Gfx.argb((int) (50 * k), myth ? Gfx.rainbow(time, i * 0.07f) & 0xFFFFFF : rgb));
            pose.popMatrix();
        }
        pose.popMatrix();

        // big card
        int ch = Math.max(80, Math.min(150, (int) (height * 0.5f)));
        int cw = ch * 20 / 29;
        float pop = Gfx.easeOutBack(a / 10f);
        int dw = Math.max(8, (int) (cw * pop)), dh = Math.max(8, (int) (ch * pop));
        drawCard(g, p, cx - dw / 2, cy + 10 - dh / 2, dw, dh, 1f, time, 3);

        // banner
        float bs = 3f * Gfx.easeOutBack(a / 10f);
        if (bs > 0.05f) {
            String banner = myth ? "MYTHICAL!" : "LEGENDARY!";
            Gfx.centered(g, font, banner, cx, Math.max(8, cy + 10 - dh / 2 - 38), bs, Gfx.argb((int) (255 * k), rgb), true);
        }
        int below = cy + 10 + dh / 2 + 6;
        Gfx.centered(g, font, p.mon().name, cx, below, 1.6f, Gfx.argb((int) (255 * k), 0xFFFFFF), true);
        Gfx.centered(g, font, p.moneyText(), cx, below + 16, 1.6f, Gfx.argb((int) (255 * k), 0x7CFFB0), true);

        // rising sparkles
        for (int i = 0; i < 36; i++) {
            float px = ((i * 73) % 100) / 100f * width;
            float py = height - ((a * (1.5f + (i % 5) * 0.4f) + i * 37) % height);
            int col = myth ? Gfx.rainbow(time, i * 0.05f) : r.color;
            Gfx.sparkle(g, (int) px, (int) py, 1 + (i % 3), Gfx.argb((int) (220 * k), col & 0xFFFFFF));
        }
    }
}
