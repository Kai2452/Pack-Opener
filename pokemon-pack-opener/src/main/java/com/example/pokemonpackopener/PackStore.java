package com.example.pokemonpackopener;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Persistent simulator state: inventory (unsold), history log and lifetime totals. */
public final class PackStore {
    public static final class Entry {
        public String name;
        public String rarity;
        public long value;
        public long time;

        public Entry() {}
    }

    private static final int HISTORY_CAP = 1000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PackStore instance = new PackStore();

    public long packsOpened;
    public long lifetimeValue;
    public long soldValue;
    public long bestValue;
    public String bestName = "";
    public List<Entry> inventory = new ArrayList<>();
    public List<Entry> history = new ArrayList<>();

    public static PackStore get() {
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("pokemon_pack_opener_data.json");
    }

    public static Entry entry(String name, String rarity, long value, long time) {
        Entry e = new Entry();
        e.name = name;
        e.rarity = rarity;
        e.value = value;
        e.time = time;
        return e;
    }

    public static void load() {
        Path p = file();
        try {
            if (Files.exists(p)) {
                PackStore s = GSON.fromJson(Files.readString(p), PackStore.class);
                if (s != null) {
                    if (s.inventory == null) s.inventory = new ArrayList<>();
                    if (s.history == null) s.history = new ArrayList<>();
                    if (s.bestName == null) s.bestName = "";
                    instance = s;
                }
            }
        } catch (Exception ex) {
            PackOpenerClient.LOG.warn("Could not read pack data, starting fresh", ex);
            instance = new PackStore();
        }
    }

    public void save() {
        try {
            Path p = file();
            Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(this));
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            PackOpenerClient.LOG.warn("Could not save pack data", ex);
        }
    }

    public void recordPulls(List<Entry> pulls) {
        for (Entry e : pulls) {
            packsOpened++;
            lifetimeValue += e.value;
            inventory.add(e);
            history.add(e);
            if (e.value > bestValue) {
                bestValue = e.value;
                bestName = e.name;
            }
        }
        if (history.size() > HISTORY_CAP) {
            history.subList(0, history.size() - HISTORY_CAP).clear();
        }
        save();
    }

    public long inventoryValue() {
        long t = 0;
        for (Entry e : inventory) t += e.value;
        return t;
    }

    /** Simulated sale: totals the inventory, adds it to soldValue, clears the inventory. */
    public long sellAll() {
        long v = inventoryValue();
        soldValue += v;
        inventory = new ArrayList<>();
        save();
        return v;
    }
}
