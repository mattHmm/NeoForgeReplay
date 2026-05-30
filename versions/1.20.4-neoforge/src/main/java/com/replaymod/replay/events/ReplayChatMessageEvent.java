package com.replaymod.replay.events;

import com.replaymod.replay.camera.CameraEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class ReplayChatMessageEvent extends Event implements ICancellableEvent {
    private final CameraEntity cameraEntity;

    public ReplayChatMessageEvent(CameraEntity cameraEntity) {
        this.cameraEntity = cameraEntity;
    }

    public CameraEntity getCameraEntity() {
        return cameraEntity;
    }
}
