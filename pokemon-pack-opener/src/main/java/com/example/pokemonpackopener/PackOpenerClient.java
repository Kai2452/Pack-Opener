package com.example.pokemonpackopener;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only. Registers one key (default: O). The mod sends no packets and never talks to the
 * server, so it is a pure local simulator.
 */
public class PackOpenerClient implements ClientModInitializer {
    public static final String MOD_ID = "pokemon_pack_opener";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        PackConfig.load();
        PackStore.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + MOD_ID + ".open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new PackOpenerScreen());
                }
            }
        });
        LOG.info("Pokemon Money Pack Opener loaded (client-side simulator).");
    }
}
