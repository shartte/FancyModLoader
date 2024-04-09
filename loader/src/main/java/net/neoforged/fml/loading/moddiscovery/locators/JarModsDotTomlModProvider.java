/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import com.mojang.logging.LogUtils;
import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.SecureJar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileParser;
import net.neoforged.fml.loading.moddiscovery.ModJarMetadata;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModProvider;
import net.neoforged.neoforgespi.locating.ModFileLoadingException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class JarModsDotTomlModProvider implements IModProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODS_TOML = "META-INF/neoforge.mods.toml";
    protected static final String MANIFEST = "META-INF/MANIFEST.MF";

    @Override
    public String name() {
        return "modsdottoml";
    }

    protected IModProvider.ModFileOrException createMod(JarContents jar) {
        var type = jar.getManifest().getMainAttributes().getValue(ModFile.TYPE);
        try {
            IModFile mod = null;
            if (jar.findFile(MODS_TOML).isPresent()) {
                LOGGER.debug(LogMarkers.SCAN, "Found {} mod of type {}: {}", MODS_TOML, type, jar.getPrimaryPath());
                var mjm = new ModJarMetadata(jar);
                mod = new ModFile(SecureJar.from(jar, mjm), this, ModFileParser::modsTomlParser);
                mjm.setModFile(mod);
            } else if (type != null) {
                LOGGER.debug(LogMarkers.SCAN, "Found {} mod of type {}: {}", MANIFEST, type, jar.getPrimaryPath());
                mod = new ModFile(SecureJar.from(jar), this, JarModsDotTomlModProvider::manifestParser, type);
            }

            return mod == null ? null : new ModFileOrException(mod, null);
        } catch (ModFileLoadingException exception) {
            return new ModFileOrException(null, exception);
        }
    }

    public static IModFileInfo manifestParser(final IModFile mod) {
        Function<String, Optional<String>> cfg = name -> Optional.ofNullable(mod.getSecureJar().moduleDataProvider().getManifest().getMainAttributes().getValue(name));
        var license = cfg.apply("LICENSE").orElse("");
        var dummy = new IConfigurable() {
            @Override
            public <T> Optional<T> getConfigElement(String... key) {
                return Optional.empty();
            }

            @Override
            public List<? extends IConfigurable> getConfigList(String... key) {
                return Collections.emptyList();
            }
        };

        return new DefaultModFileInfo(mod, license, dummy);
    }

    @Override
    public boolean isValid(final IModFile modFile) {
        return true;
    }

    @Override
    public @Nullable ModFileOrException provide(JarContents jar) {
        return createMod(jar);
    }

    private record DefaultModFileInfo(IModFile mod, String license,
            IConfigurable configurable) implements IModFileInfo, IConfigurable {
        @Override
        public <T> Optional<T> getConfigElement(final String... strings) {
            return Optional.empty();
        }

        @Override
        public List<? extends IConfigurable> getConfigList(final String... strings) {
            return null;
        }

        @Override
        public List<IModInfo> getMods() {
            return Collections.emptyList();
        }

        @Override
        public List<LanguageSpec> requiredLanguageLoaders() {
            return Collections.emptyList();
        }

        @Override
        public boolean showAsResourcePack() {
            return false;
        }

        @Override
        public boolean showAsDataPack() {
            return false;
        }

        @Override
        public Map<String, Object> getFileProperties() {
            return Collections.emptyMap();
        }

        @Override
        public String getLicense() {
            return license;
        }

        @Override
        public IModFile getFile() {
            return mod;
        }

        @Override
        public IConfigurable getConfig() {
            return configurable;
        }

        // These Should never be called as it's only called from ModJarMetadata.version and we bypass that
        @Override
        public String moduleName() {
            return mod.getSecureJar().name();
        }

        @Override
        public String versionString() {
            return null;
        }

        @Override
        public List<String> usesServices() {
            return null;
        }

        @Override
        public String toString() {
            return "IModFileInfo(" + mod.getFilePath() + ")";
        }
    }
}
