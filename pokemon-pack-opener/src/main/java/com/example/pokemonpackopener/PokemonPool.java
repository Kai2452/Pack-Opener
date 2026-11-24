package com.example.pokemonpackopener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PokemonPool {
    private static final List<Pokemon> ALL = new ArrayList<>();
    private static final Map<Rarity, List<Pokemon>> BY_RARITY = new EnumMap<>(Rarity.class);
    private static final Map<String, Pokemon> BY_NAME = new HashMap<>();

    private PokemonPool() {}

    private static void p(Rarity r, String n, PType a, PType b) {
        Pokemon m = new Pokemon(n, r, a, b);
        ALL.add(m);
        BY_RARITY.computeIfAbsent(r, k -> new ArrayList<>()).add(m);
        BY_NAME.put(n, m);
    }

    static {
        Rarity c = Rarity.COMMON, u = Rarity.UNCOMMON, ra = Rarity.RARE,
                e = Rarity.EPIC, l = Rarity.LEGENDARY, m = Rarity.MYTHICAL;

        p(c, "Pidgey", PType.NORMAL, PType.FLYING);
        p(c, "Rattata", PType.NORMAL, null);
        p(c, "Caterpie", PType.BUG, null);
        p(c, "Magikarp", PType.WATER, null);
        p(c, "Zubat", PType.POISON, PType.FLYING);
        p(c, "Geodude", PType.ROCK, PType.GROUND);
        p(c, "Weedle", PType.BUG, PType.POISON);
        p(c, "Sentret", PType.NORMAL, null);

        p(u, "Pikachu", PType.ELECTRIC, null);
        p(u, "Eevee", PType.NORMAL, null);
        p(u, "Growlithe", PType.FIRE, null);
        p(u, "Psyduck", PType.WATER, null);
        p(u, "Jigglypuff", PType.NORMAL, PType.FAIRY);
        p(u, "Vulpix", PType.FIRE, null);
        p(u, "Machop", PType.FIGHTING, null);
        p(u, "Oddish", PType.GRASS, PType.POISON);

        p(ra, "Lucario", PType.FIGHTING, PType.STEEL);
        p(ra, "Gengar", PType.GHOST, PType.POISON);
        p(ra, "Scyther", PType.BUG, PType.FLYING);
        p(ra, "Gyarados", PType.WATER, PType.FLYING);
        p(ra, "Arcanine", PType.FIRE, null);
        p(ra, "Alakazam", PType.PSYCHIC, null);
        p(ra, "Snorlax", PType.NORMAL, null);
        p(ra, "Ampharos", PType.ELECTRIC, null);

        p(e, "Charizard", PType.FIRE, PType.FLYING);
        p(e, "Blastoise", PType.WATER, null);
        p(e, "Venusaur", PType.GRASS, PType.POISON);
        p(e, "Dragonite", PType.DRAGON, PType.FLYING);
        p(e, "Tyranitar", PType.ROCK, PType.DARK);
        p(e, "Garchomp", PType.DRAGON, PType.GROUND);
        p(e, "Salamence", PType.DRAGON, PType.FLYING);
        p(e, "Metagross", PType.STEEL, PType.PSYCHIC);

        p(l, "Articuno", PType.ICE, PType.FLYING);
        p(l, "Zapdos", PType.ELECTRIC, PType.FLYING);
        p(l, "Moltres", PType.FIRE, PType.FLYING);
        p(l, "Lugia", PType.PSYCHIC, PType.FLYING);
        p(l, "Ho-Oh", PType.FIRE, PType.FLYING);
        p(l, "Rayquaza", PType.DRAGON, PType.FLYING);
        p(l, "Kyogre", PType.WATER, null);
        p(l, "Groudon", PType.GROUND, null);

        p(m, "Mew", PType.PSYCHIC, null);
        p(m, "Celebi", PType.PSYCHIC, PType.GRASS);
        p(m, "Jirachi", PType.STEEL, PType.PSYCHIC);
        p(m, "Deoxys", PType.PSYCHIC, null);
        p(m, "Manaphy", PType.WATER, null);
        p(m, "Darkrai", PType.DARK, null);
        p(m, "Shaymin", PType.GRASS, null);
        p(m, "Arceus", PType.NORMAL, null);
    }

    public static List<Pokemon> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static List<Pokemon> of(Rarity r) {
        return BY_RARITY.get(r);
    }

    public static Pokemon byName(String name) {
        return BY_NAME.get(name);
    }
}
