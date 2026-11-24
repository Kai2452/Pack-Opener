package com.example.pokemonpackopener;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** All sound effects use vanilla sounds, so there are no audio files to ship. */
final class Sfx {
    private Sfx() {}

    static void play(SoundEvent ev, float pitch) {
        PackConfig c = PackConfig.get();
        if (!c.soundEnabled) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, c.soundVolume));
    }

    static void play(Holder<SoundEvent> ev, float pitch) {
        PackConfig c = PackConfig.get();
        if (!c.soundEnabled) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, c.soundVolume));
    }

    static void click() {
        play(SoundEvents.UI_BUTTON_CLICK, 1.0f);
    }

    static void tear() {
        play(SoundEvents.BOOK_PAGE_TURN, 0.6f);
    }

    static void rattle(float prog) {
        play(SoundEvents.UI_BUTTON_CLICK, 0.5f + prog * 1.2f);
    }

    static void burst() {
        play(SoundEvents.FIREWORK_ROCKET_BLAST, 1.2f);
    }

    static void sell() {
        play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.5f);
        play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.8f);
    }

    static void reveal(Rarity r) {
        switch (r) {
            case COMMON -> play(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f);
            case UNCOMMON -> play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f);
            case RARE -> {
                play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.25f);
                play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f);
            }
            case EPIC -> {
                play(SoundEvents.PLAYER_LEVELUP, 1.4f);
                play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f);
            }
            case LEGENDARY -> {
                play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);
                play(SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f);
                play(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.6f);
            }
            case MYTHICAL -> {
                play(SoundEvents.ENDER_DRAGON_GROWL, 1.7f);
                play(SoundEvents.BEACON_ACTIVATE, 1.5f);
                play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.4f);
                play(SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f);
            }
        }
    }
}
