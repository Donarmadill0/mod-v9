package com.godvoice;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;

public class GodVoiceClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                GodClientCommands.register(dispatcher));

        // god messages arrive as normal chat/whispers; hide them and show the dialogue box instead
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) ->
                !GodChat.handle(message.getString()));
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                !GodChat.handle(message.getString()));

        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
                Identifier.of("godvoice", "dialogue"), DialogueOverlay::render);
    }
}
