package com.replaymod.core;

import com.replaymod.core.versions.LangResourcePack;
import net.minecraft.SharedConstants;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

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
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        mod.getKeyBindingRegistry().getBindings().values()
                .forEach(binding -> event.register(binding.keyBinding));
    }

    private void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
            ResourcePackProfile langPack = ResourcePackProfile.create(
                    LangResourcePack.NAME,
                    Text.literal("ReplayMod Translations"),
                    false,
                    new ResourcePackProfile.PackFactory() {
                        public ResourcePack open(String name) { return new LangResourcePack(); }
                        public ResourcePack openWithOverlays(String name, ResourcePackProfile.Metadata m) { return open(name); }
                    },
                    ResourceType.CLIENT_RESOURCES,
                    ResourcePackProfile.InsertionPosition.TOP,
                    ResourcePackSource.BUILTIN
            );
            if (langPack != null) {
                event.addRepositorySource(consumer -> consumer.accept(langPack));
            }
            if (ReplayMod.jGuiResourcePack != null) {
                final ResourcePack jguiPack0 = ReplayMod.jGuiResourcePack;
                ResourcePackProfile jguiPack = ResourcePackProfile.create(
                        ReplayMod.JGUI_RESOURCE_PACK_NAME,
                        Text.literal("jGui Resources (dev)"),
                        false,
                        new ResourcePackProfile.PackFactory() {
                            public ResourcePack open(String name) { return jguiPack0; }
                            public ResourcePack openWithOverlays(String name, ResourcePackProfile.Metadata m) { return open(name); }
                        },
                        ResourceType.CLIENT_RESOURCES,
                        ResourcePackProfile.InsertionPosition.TOP,
                        ResourcePackSource.BUILTIN
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
        return SharedConstants.getGameVersion().getId();
    }

    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }
}
