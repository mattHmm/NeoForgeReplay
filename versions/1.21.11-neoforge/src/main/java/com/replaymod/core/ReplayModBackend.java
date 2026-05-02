package com.replaymod.core;

import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.versions.LangResourcePack;
import net.minecraft.SharedConstants;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackPosition;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
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
        modEventBus.addListener(this::registerKeyMappings);
        modEventBus.addListener(this::addPackFinders);
        modEventBus.addListener(this::registerPayloadHandlers);
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(Restrictions.ID, Restrictions.CODEC, (payload, context) -> {});
        registrar.configurationToClient(Restrictions.ID, Restrictions.CODEC, (payload, context) -> {});
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        mod.getKeyBindingRegistry().getBindings().values()
                .forEach(binding -> event.register(binding.keyBinding));
    }

    private void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
            ResourcePackProfile langPack = ResourcePackProfile.create(
                    new ResourcePackInfo(LangResourcePack.NAME, Text.literal("ReplayMod Translations"), ResourcePackSource.BUILTIN, Optional.empty()),
                    new ResourcePackProfile.PackFactory() {
                        public ResourcePack open(ResourcePackInfo i) { return new LangResourcePack(); }
                        public ResourcePack openWithOverlays(ResourcePackInfo i, ResourcePackProfile.Metadata m) { return open(i); }
                    },
                    ResourceType.CLIENT_RESOURCES,
                    new ResourcePackPosition(false, ResourcePackProfile.InsertionPosition.TOP, false)
            );
            if (langPack != null) {
                event.addRepositorySource(consumer -> consumer.accept(langPack));
            }
            if (ReplayMod.jGuiResourcePack != null) {
                final ResourcePack jguiPack0 = ReplayMod.jGuiResourcePack;
                ResourcePackProfile jguiPack = ResourcePackProfile.create(
                        new ResourcePackInfo(ReplayMod.JGUI_RESOURCE_PACK_NAME, Text.literal("jGui Resources (dev)"), ResourcePackSource.BUILTIN, Optional.empty()),
                        new ResourcePackProfile.PackFactory() {
                            public ResourcePack open(ResourcePackInfo i) { return jguiPack0; }
                            public ResourcePack openWithOverlays(ResourcePackInfo i, ResourcePackProfile.Metadata m) { return open(i); }
                        },
                        ResourceType.CLIENT_RESOURCES,
                        new ResourcePackPosition(false, ResourcePackProfile.InsertionPosition.TOP, false)
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
        return SharedConstants.getGameVersion().id();
    }

    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }
}
