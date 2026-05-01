package com.replaymod.recording;

import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.Module;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.recording.handler.ConnectionEventHandler;
import com.replaymod.recording.handler.GuiHandler;
import com.replaymod.recording.mixin.NetworkManagerAccessor;
import com.replaymod.recording.packet.PacketListener;
import com.replaymod.replay.ReplayHandler;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import net.minecraft.network.ClientConnection;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReplayModRecording implements Module {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AttributeKey<Void> ATTR_CHECKED = AttributeKey.newInstance("ReplayModRecording_checked");

    { instance = this; }
    public static ReplayModRecording instance;

    private ReplayMod core;

    private ConnectionEventHandler connectionEventHandler;

    public ReplayModRecording(ReplayMod mod) {
        core = mod;

        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("replaymod.input.marker", Keyboard.KEY_M, new Runnable() {
            @Override
            public void run() {
                PacketListener packetListener = connectionEventHandler.getPacketListener();
                if (packetListener != null) {
                    packetListener.addMarker(null);
                    core.printInfoToChat("replaymod.chat.addedmarker");
                }
            }
        }, false);
    }

    @Override
    public void initClient() {
        connectionEventHandler = new ConnectionEventHandler(LOGGER, core);

        new GuiHandler(core).register();

        // Register the restrictions payload so NeoForge does not close the connection when the
        // server sends it.  We receive it via the low-level Netty pipeline (MixinClientConnection)
        // rather than this handler, so the handler is intentionally empty.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (RegisterPayloadHandlersEvent event) -> {
                    PayloadRegistrar registrar = event.registrar("1");
                    registrar.playToClient(Restrictions.ID, Restrictions.CODEC, (payload, context) -> {});
                    registrar.configurationToClient(Restrictions.ID, Restrictions.CODEC, (payload, context) -> {});
                });
    }

    public void initiateRecording(ClientConnection networkManager) {
        Channel channel = ((NetworkManagerAccessor) networkManager).getChannel();
        if (channel.pipeline().get(ReplayHandler.PACKET_HANDLER_NAME) != null) return;
        if (channel.hasAttr(ATTR_CHECKED)) return;
        channel.attr(ATTR_CHECKED).set(null);
        connectionEventHandler.onConnectedToServerEvent(networkManager);
    }

    public ConnectionEventHandler getConnectionEventHandler() {
        return connectionEventHandler;
    }
}
