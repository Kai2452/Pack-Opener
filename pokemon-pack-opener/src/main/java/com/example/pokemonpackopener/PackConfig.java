package com.example.pokemonpackopener;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * config/pokemon_pack_opener.json
 * Everything shown in the UI as a price comes from here, so you can match real DonutSMP sell prices.
 */
public final class PackConfig {
    public static final class RarityCfg {
        public double weight;
        public long min;
        public long max;

        public RarityCfg() {}

        public RarityCfg(double weight, long min, long max) {
            this.weight = weight;
            this.min = min;
            this.max = max;
        }
    }

    public String _readme = "Visual simulator only. Prices are fake display values - edit them to match DonutSMP /sell prices. "
            + "weight = relative odds (they are normalised to 100%). priceMultiplier scales every min/max. "
            + "priceOverrides sets an exact $ for a named Pokemon (e.g. \"Pikachu\": 42000) and ignores the multiplier.";
    public double priceMultiplier = 1.0;
    public int valueRounding = 100;
    public boolean soundEnabled = true;
    public float soundVolume = 1.0f;
    public boolean flashEffects = true;
    public boolean screenShake = true;
    public Map<String, RarityCfg> rarities = new LinkedHashMap<>();
    public Map<String, Long> priceOverrides = new LinkedHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static PackConfig instance = defaults();

    public static PackConfig get() {
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("pokemon_pack_opener.json");
    }

    private static PackConfig defaults() {
        PackConfig c = new PackConfig();
        c.sanitize();
        return c;
    }

    public RarityCfg cfg(Rarity r) {
        return rarities.get(r.key());
    }

    public double totalWeight() {
        double t = 0;
        for (Rarity r : Rarity.values()) {
            t += cfg(r).weight;
        }
        return t;
    }

    public void sanitize() {
        for (Rarity r : Rarity.values()) {
            RarityCfg rc = rarities.get(r.key());
            if (rc == null) {
                rc = new RarityCfg(r.defaultWeight, r.defaultMin, r.defaultMax);
                rarities.put(r.key(), rc);
            }
            if (rc.weight < 0 || Double.isNaN(rc.weight)) rc.weight = 0;
            if (rc.min < 0) rc.min = 0;
            if (rc.max < rc.min) {
                long t = rc.min;
                rc.min = rc.max;
                rc.max = t;
            }
        }
        if (totalWeight() <= 0) {
            for (Rarity r : Rarity.values()) cfg(r).weight = r.defaultWeight;
        }
        if (!(priceMultiplier > 0) || priceMultiplier > 1000) priceMultiplier = 1.0;
        if (valueRounding < 1) valueRounding = 1;
        if (soundVolume < 0f) soundVolume = 0f;
        if (soundVolume > 2f) soundVolume = 2f;
        if (priceOverrides == null) priceOverrides = new LinkedHashMap<>();
    }

    public static void load() {
        Path p = file();
        try {
            if (Files.exists(p)) {
                PackConfig c = GSON.fromJson(Files.readString(p), PackConfig.class);
                if (c != null) {
                    if (c.rarities == null) c.rarities = new LinkedHashMap<>();
                    c.sanitize();
                    instance = c;
                }
            } else {
                instance = defaults();
            }
            instance.save();
        } catch (Exception ex) {
            PackOpenerClient.LOG.warn("Could not read pokemon_pack_opener.json, using defaults (file left untouched)", ex);
            instance = defaults();
        }
    }

    public void save() {
        try {
            Files.writeString(file(), GSON.toJson(this));
        } catch (Exception ex) {
            PackOpenerClient.LOG.warn("Could not write config", ex);
        }
    }
}
