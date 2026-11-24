package com.example.pokemonpackopener;

import java.util.Random;

/**
 * One reward. Each Pokémon gets a unique, deterministic 8x8 symmetric pixel-art
 * "creature badge" generated from its name and tinted with its type colours.
 * No texture files are needed, which keeps the mod tiny and fast.
 */
public final class Pokemon {
    public final String name;
    public final Rarity rarity;
    public final PType t1;
    public final PType t2; // may be null

    /** 64 cells: 0 empty, 1 primary, 2 secondary, 3 eye. */
    public final byte[] grid = new byte[64];

    public Pokemon(String name, Rarity rarity, PType t1, PType t2) {
        this.name = name;
        this.rarity = rarity;
        this.t1 = t1;
        this.t2 = t2;
        build();
    }

    private void set(int x, int y, int v) {
        grid[y * 8 + x] = (byte) v;
        grid[y * 8 + 7 - x] = (byte) v;
    }

    private void build() {
        Random r = new Random(name.hashCode() * 2654435761L + 12345L);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 4; x++) {
                float cx = x - 3.5f;
                float cy = y - 3.5f;
                float d = (cx * cx) / 14f + (cy * cy) / 16f;
                int chance = d < 0.45f ? 92 : (d < 0.8f ? 62 : 28);
                int v = 0;
                if (r.nextInt(100) < chance) {
                    v = r.nextInt(100) < 32 ? 2 : 1;
                }
                set(x, y, v);
            }
        }
        for (int y = 1; y <= 6; y++) {
            for (int x = 2; x <= 3; x++) {
                if (grid[y * 8 + x] == 0) {
                    set(x, y, 1);
                }
            }
        }
        set(2, 3, 3); // eyes
    }
}
