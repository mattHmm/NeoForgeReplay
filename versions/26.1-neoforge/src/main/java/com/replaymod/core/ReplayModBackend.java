package com.replaymod.core;

import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.versions.LangResourcePack;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Optional;

import static com.replaymod.core.ReplayMod.MOD_ID;

@Mod(MOD_ID)
public class ReplayModBackend {
    private final ReplayMod mod = new ReplayMod(this);

    public ReplayModBackend(IEventBus modEventBus) {
        mod.initModules();
        NeoForgeEventBridge.register();
        new KonamiCode().register();
        modEventBus.addListener(this::registerKeyMappings);
        modEventBus.addListener(this::addPackFinders);
        modEventBus.addListener(this::registerPayloadHandlers);
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(Restrictions.ID, Restrictions.CODEC, (payload, context) -> {});
        registrar.configurationToClient(Restrictions.ID, Restrictions.CODEC, (payload, context) -> {});
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        mod.getKeyBindingRegistry().getBindings().values()
                .forEach(binding -> event.register(binding.keyBinding));
    }

    private void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Pack langPack = Pack.readMetaAndCreate(
                    new PackLocationInfo(LangResourcePack.NAME, Component.literal("ReplayMod Translations"), PackSource.BUILT_IN, Optional.empty()),
                    new Pack.ResourcesSupplier() {
                        public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo i) { return new LangResourcePack(); }
                        public net.minecraft.server.packs.PackResources openFull(PackLocationInfo i, Pack.Metadata m) { return openPrimary(i); }
                    },
                    PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(false, Pack.Position.TOP, false)
            );
            if (langPack != null) {
                event.addRepositorySource(consumer -> consumer.accept(langPack));
            }
            if (ReplayMod.jGuiResourcePack != null) {
                final net.minecraft.server.packs.PackResources jguiPack0 = ReplayMod.jGuiResourcePack;
                Pack jguiPack = Pack.readMetaAndCreate(
                        new PackLocationInfo(ReplayMod.JGUI_RESOURCE_PACK_NAME, Component.literal("jGui Resources (dev)"), PackSource.BUILT_IN, Optional.empty()),
                        new Pack.ResourcesSupplier() {
                            public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo i) { return jguiPack0; }
                            public net.minecraft.server.packs.PackResources openFull(PackLocationInfo i, Pack.Metadata m) { return openPrimary(i); }
                        },
                        PackType.CLIENT_RESOURCES,
                        new PackSelectionConfig(false, Pack.Position.TOP, false)
                );
                if (jguiPack != null) {
                    event.addRepositorySource(consumer -> consumer.accept(jguiPack));
                }
            }
        }
    }

    public String getVersion() {
        return ModList.get().getModContainerById(MOD_ID)
                .orElseThrow(IllegalStateException::new)
                .getModInfo().getVersion().toString();
    }

    public String getMinecraftVersion() {
        return SharedConstants.getCurrentVersion().id();
    }

    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }
}
