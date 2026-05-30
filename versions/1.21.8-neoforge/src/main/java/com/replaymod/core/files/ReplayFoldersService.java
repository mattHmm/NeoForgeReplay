package com.replaymod.core.files;

import com.google.common.net.PercentEscaper;
import com.replaymod.core.Setting;
import com.replaymod.core.SettingsRegistry;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.replaymod.core.utils.Utils.ensureDirectoryExists;

public class ReplayFoldersService {
    // Use FMLPaths.GAMEDIR instead of Minecraft.getInstance().runDirectory so this
    // can be resolved during @Mod construction before the Minecraft instance exists.
    private final Path mcDir = FMLPaths.GAMEDIR.get();
    private final SettingsRegistry settings;

    public ReplayFoldersService(SettingsRegistry settings) {
        this.settings = settings;
    }

    public Path getReplayFolder() throws IOException {
        return ensureDirectoryExists(mcDir.resolve(settings.get(Setting.RECORDING_PATH)));
    }

    public Path getRawReplayFolder() throws IOException {
        return ensureDirectoryExists(getReplayFolder().resolve("raw"));
    }

    public Path getRecordingFolder() throws IOException {
        return ensureDirectoryExists(getReplayFolder().resolve("recording"));
    }

    public Path getCacheFolder() throws IOException {
        Path path = ensureDirectoryExists(mcDir.resolve(settings.get(Setting.CACHE_PATH)));
        try {
            Files.setAttribute(path, "dos:hidden", true);
        } catch (UnsupportedOperationException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    private static final PercentEscaper CACHE_FILE_NAME_ENCODER = new PercentEscaper("-_ ", false);

    public Path getCachePathForReplay(Path replay) throws IOException {
        Path replayFolder = getReplayFolder();
        Path cacheFolder = getCacheFolder();
        Path relative = replayFolder.toAbsolutePath().relativize(replay.toAbsolutePath());
        return cacheFolder.resolve(CACHE_FILE_NAME_ENCODER.escape(relative.toString()));
    }

    public Path getReplayPathForCache(Path cache) throws IOException {
        String relative = URLDecoder.decode(cache.getFileName().toString(), "UTF-8");
        Path replayFolder = getReplayFolder();
        return replayFolder.resolve(relative);
    }
}
