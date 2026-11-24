package com.example.pokemonpackopener;

/** Reward tiers with their default odds (%) and default sell-value ranges ($). */
public enum Rarity {
    COMMON("Common", 0xFFB8C0CC, 15, 5_000L, 20_000L),
    UNCOMMON("Uncommon", 0xFF55E07A, 25, 20_000L, 50_000L),
    RARE("Rare", 0xFF4DA3FF, 25, 50_000L, 100_000L),
    EPIC("Epic", 0xFFB45CFF, 20, 100_000L, 250_000L),
    LEGENDARY("Legendary", 0xFFFFC21A, 12, 250_000L, 750_000L),
    MYTHICAL("Mythical", 0xFFFF5FD2, 3, 750_000L, 2_000_000L);

    public final String label;
    public final int color;
    public final double defaultWeight;
    public final long defaultMin;
    public final long defaultMax;

    Rarity(String label, int color, double w, long min, long max) {
        this.label = label;
        this.color = color;
        this.defaultWeight = w;
        this.defaultMin = min;
        this.defaultMax = max;
    }

    public boolean isCinematic() {
        return this == LEGENDARY || this == MYTHICAL;
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
