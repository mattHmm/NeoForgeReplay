package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public class Mixin_ShowSpectatedEntityInReplay {
    // In MC 26.1, extractVisibleEntities skips entity == camera.entity() when !camera.isDetached().
    // In older MC the skip compared against mc.player (always CameraEntity), so the recording player
    // was never skipped. Here we return true ("detached") during replay so the spectated entity renders.
    @Redirect(
        method = "extractVisibleEntities",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z")
    )
    private boolean replaymod_treatCameraAsDetachedForEntitySkip(Camera camera) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            return true;
        }
        return camera.isDetached();
    }
}
