package com.replaymod.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class KonamiCode {

    private static final List<Integer> SEQUENCE = List.of(
            GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_UP,
            GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_DOWN,
            GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
            GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
            GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_A
    );

    private int position = 0;

    public void register() {
        NeoForge.EVENT_BUS.addListener(this::onKey);
    }

    private void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        int key = event.getKey();
        if (key == SEQUENCE.get(position)) {
            position++;
            if (position == SEQUENCE.size()) {
                position = 0;
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    mc.player.sendMessage(Text.literal("Made by Fishybeing"), false);
                }
            }
        } else {
            position = (key == SEQUENCE.get(0)) ? 1 : 0;
        }
    }
}
