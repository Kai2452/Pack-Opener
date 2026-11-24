package com.example.pokemonpackopener;

/** One rolled reward with a ready-formatted money string. */
public record Pull(Pokemon mon, long value, String moneyText) {}
