package com.replaymod.core.versions;

import com.google.gson.Gson;
import com.replaymod.core.ReplayMod;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.neoforged.fml.ModList;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static de.johni0702.minecraft.gui.versions.MCVer.identifier;

/**
 * Resource pack which on-the-fly converts pre-1.13 language files into 1.13 json format.
 * Also duplicates {@code replaymod.input.*} bindings to {@code key.replaymod.*} as convention.
 */
public class LangResourcePack extends AbstractFileResourcePack {
    private static final Gson GSON = new Gson();
    public static final String NAME = "replaymod_lang";
    private static final Pattern JSON_FILE_PATTERN = Pattern.compile("^assets/" + ReplayMod.MOD_ID + "/lang/([a-z][a-z])_([a-z][a-z]).json$");
    private static final Pattern LANG_FILE_NAME_PATTERN = Pattern.compile("^([a-zA-Z]{2})_([a-zA-Z]{2}).lang$");

    public static final String LEGACY_KEY_PREFIX = "replaymod.input.";
    private static final String FABRIC_KEY_FORMAT = "key." + ReplayMod.MOD_ID + ".%s";

    private final Path basePath;

    public LangResourcePack() {
        super(new ResourcePackInfo(NAME, Text.literal("ReplayMod Translations"), ResourcePackSource.NONE, Optional.empty()));
        this.basePath = ModList.get().getModFileById(ReplayMod.MOD_ID).getFile().getContents().getContentRoots().stream().findFirst().orElseThrow();
    }

    private String langName(String path) {
        Matcher matcher = JSON_FILE_PATTERN.matcher(path);
        if (!matcher.matches()) return null;
        return String.format("%s_%s.lang", matcher.group(1), matcher.group(2).toUpperCase());
    }

    private Path baseLangPath() {
        return basePath.resolve("assets").resolve(ReplayMod.MOD_ID).resolve("lang");
    }

    private Path langPath(String path) {
        String langName = langName(path);
        if (langName == null) return null;
        return baseLangPath().resolve(langName);
    }

    @Override
    public net.minecraft.resource.InputSupplier<InputStream> openRoot(String... segments) {
        byte[] bytes;
        try {
            bytes = readFile(String.join("/", segments));
        } catch (IOException e) {
            return null;
        }
        if (bytes == null) {
            return null;
        }
        return () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public net.minecraft.resource.InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        return openRoot(type.getDirectory(), id.getNamespace(), id.getPath());
    }

    private byte[] readFile(String path) throws IOException {
        if ("pack.mcmeta".equals(path)) {
            return "{\"pack\": {\"description\": \"ReplayMod language files\", \"pack_format\": 34}}".getBytes(StandardCharsets.UTF_8);
        }

        Path langPath = langPath(path);
        if (langPath == null) return null;

        List<String> langFile;
        try (InputStream in = Files.newInputStream(langPath)) {
            langFile = IOUtils.readLines(in, StandardCharsets.UTF_8);
        }

        Map<String, String> properties = new HashMap<>();
        for (String line : langFile) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            int i = line.indexOf('=');
            String key = line.substring(0, i);
            String value = line.substring(i + 1);
            if (key.startsWith(LEGACY_KEY_PREFIX)) {
                properties.put(key, value);
                key = String.format(FABRIC_KEY_FORMAT, key.substring(LEGACY_KEY_PREFIX.length()));
            }
            properties.put(key, value);
        }

        return GSON.toJson(properties).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {
        findResources(type, prefix, id -> consumer.accept(id, () -> {
            try {
                byte[] bytes = readFile(type.getDirectory() + "/" + id.getNamespace() + "/" + id.getPath());
                if (bytes == null) throw new IOException("Resource not found: " + id);
                return new ByteArrayInputStream(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private void findResources(ResourceType type, String path, Consumer<Identifier> consumer) {
        if (type != ResourceType.CLIENT_RESOURCES) return;
        if (!"lang".equals(path)) return;
        Path base = baseLangPath();
        try (Stream<Path> stream = Files.walk(base, 1)) {
            stream
                    .skip(1)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName).map(Path::toString)
                    .map(LANG_FILE_NAME_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> String.format("%s_%s.json", matcher.group(1).toLowerCase(), matcher.group(2).toLowerCase()))
                    .map(name -> identifier(ReplayMod.MOD_ID, "lang/" + name))
                    .forEach(consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> getNamespaces(ResourceType resourcePackType) {
        if (resourcePackType == ResourceType.CLIENT_RESOURCES) {
            return Collections.singleton("replaymod");
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void close() {}
}
