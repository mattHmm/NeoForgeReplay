package com.replaymod.core.utils;

import com.replaymod.replaystudio.data.ModInfo;
import net.neoforged.fml.ModList;

import java.util.Collection;
import java.util.stream.Collectors;

class ModInfoGetter {
    static Collection<ModInfo> getInstalledNetworkMods() {
        return ModList.get().getMods().stream()
                .map(m -> new ModInfo(m.getModId(), m.getDisplayName(), m.getVersion().toString()))
                .collect(Collectors.toList());
    }
}
