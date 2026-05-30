package com.replaymod.core;

import de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
import de.johni0702.minecraft.gui.versions.callbacks.OpenGuiScreenCallback;
import de.johni0702.minecraft.gui.versions.callbacks.PostRenderScreenCallback;
import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
import de.johni0702.minecraft.gui.versions.callbacks.RenderHudCallback;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import de.johni0702.minecraft.gui.function.Click;
import de.johni0702.minecraft.gui.versions.callbacks.MouseCallback;
import net.neoforged.neoforge.event.TickEvent;

import java.util.ArrayList;

public class NeoForgeEventBridge {

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onScreenInitPre);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onScreenInitPost);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onClientTick);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onRenderGui);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onScreenRenderPost);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onScreenMouseDown);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onScreenMouseUp);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onScreenMouseDragged);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onScreenMouseScrolled);
    }

    private static void onScreenInitPre(ScreenEvent.Init.Pre event) {
        InitScreenCallback.Pre.EVENT.invoker().preInitScreen(event.getScreen());
    }

    private static void onScreenInitPost(ScreenEvent.Init.Post event) {
        InitScreenCallback.EVENT.invoker().initScreen(event.getScreen(), new ScreenButtonList(event));
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            PreTickCallback.EVENT.invoker().preTick();
        }
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        OpenGuiScreenCallback.EVENT.invoker().openGuiScreen(event.getScreen());
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        RenderHudCallback.EVENT.invoker().renderHud(
                event.getGuiGraphics(),
                event.getPartialTick());
    }

    private static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        PostRenderScreenCallback.EVENT.invoker().postRenderScreen(
                event.getGuiGraphics(),
                event.getPartialTick());
    }


    private static void onScreenMouseDown(ScreenEvent.MouseButtonPressed.Pre event) {
        if (MouseCallback.EVENT.invoker().mouseDown(new Click(event.getMouseX(), event.getMouseY(), event.getButton()))) {
            event.setCanceled(true);
        }
    }

    private static void onScreenMouseUp(ScreenEvent.MouseButtonReleased.Pre event) {
        if (MouseCallback.EVENT.invoker().mouseUp(new Click(event.getMouseX(), event.getMouseY(), event.getButton()))) {
            event.setCanceled(true);
        }
    }

    private static void onScreenMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (MouseCallback.EVENT.invoker().mouseDrag(new Click(event.getMouseX(), event.getMouseY(), event.getMouseButton()), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    private static void onScreenMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (MouseCallback.EVENT.invoker().mouseScroll(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    private static class ScreenButtonList extends ArrayList<ClickableWidget> {
        private final ScreenEvent.Init event;

        ScreenButtonList(ScreenEvent.Init event) {
            this.event = event;
            event.getListenersList().stream()
                    .filter(e -> e instanceof ClickableWidget)
                    .map(e -> (ClickableWidget) e)
                    .forEach(super::add);
        }

        @Override
        public boolean add(ClickableWidget widget) {
            event.addListener(widget);
            return super.add(widget);
        }

        @Override
        public void add(int index, ClickableWidget widget) {
            event.addListener(widget);
            super.add(index, widget);
        }
    }
}
