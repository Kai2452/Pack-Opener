package com.example.pokemonpackopener;

import java.util.List;
import java.util.Random;

public final class PackRoller {
    private PackRoller() {}

    public static Pull roll(PackConfig cfg, Random rng) {
        double total = cfg.totalWeight();
        double pick = rng.nextDouble() * total;
        Rarity chosen = Rarity.COMMON;
        double acc = 0;
        for (Rarity r : Rarity.values()) {
            acc += cfg.cfg(r).weight;
            if (pick < acc) {
                chosen = r;
                break;
            }
        }
        List<Pokemon> pool = PokemonPool.of(chosen);
        Pokemon mon = pool.get(rng.nextInt(pool.size()));

        long value;
        Long override = cfg.priceOverrides.get(mon.name);
        if (override != null && override >= 0) {
            value = override;
        } else {
            PackConfig.RarityCfg rc = cfg.cfg(chosen);
            long span = rc.max - rc.min;
            long base = rc.min + (long) (rng.nextDouble() * (span + 1));
            value = Math.round(base * cfg.priceMultiplier);
            long unit = cfg.valueRounding;
            value = Math.round(value / (double) unit) * unit;
        }
        return new Pull(mon, value, Gfx.money(value));
    }
}
