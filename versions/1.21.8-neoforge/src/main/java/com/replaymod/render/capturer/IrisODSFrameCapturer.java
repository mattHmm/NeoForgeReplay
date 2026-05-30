package com.replaymod.render.capturer;

import com.replaymod.render.frame.ODSOpenGlFrame;
import com.replaymod.render.rendering.Channel;
import com.replaymod.render.rendering.FrameCapturer;

import java.util.Map;

/**
 * Iris ODS rendering is not supported on NeoForge (Oculus has different internals).
 */
public class IrisODSFrameCapturer implements FrameCapturer<ODSOpenGlFrame> {

    public static final String SHADER_PACK_NAME = "assets/replaymod/iris/ods";

    public IrisODSFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, int frameSize) {
        throw new UnsupportedOperationException("Iris ODS rendering is not supported on NeoForge");
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public Map<Channel, ODSOpenGlFrame> process() {
        return null;
    }

    @Override
    public void close() {}

    public boolean isLeftEye() { return false; }
    public int getDirection() { return 0; }
}
