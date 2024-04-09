/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import com.electronwill.nightconfig.core.Config;
import com.mojang.logging.LogUtils;
import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.JarContentsBuilder;
import cpw.mods.jarhandling.SecureJar;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.neoforged.fml.loading.ClasspathTransformerDiscoverer;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModJarMetadata;
import net.neoforged.fml.loading.moddiscovery.NightConfigWrapper;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModLocator;
import net.neoforged.neoforgespi.locating.IModProvider;
import net.neoforged.neoforgespi.locating.ModFileFactory;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class MinecraftLocator implements IModProvider, IModLocator {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public @Nullable ModFileOrException provide(JarContents jar) {
        return null;
    }

    @Override
    public Stream<JarContents> scanCandidates() {
        final var launchHandler = FMLLoader.getLaunchHandler();
        var baseMC = launchHandler.getMinecraftPaths();
        var otherModsExcluded = ClasspathTransformerDiscoverer.allExcluded();
        var othermods = baseMC.otherModPaths().stream()
                .filter(p -> p.stream().noneMatch(otherModsExcluded::contains)) //We cannot load MOD_CLASSES from the classpath if they are loaded on the SERVICE layer.
                .map(set -> new JarContentsBuilder().paths(set.toArray(Path[]::new)).build());
        var artifacts = baseMC.otherArtifacts().stream()
                .map(pt -> new JarContentsBuilder().paths(pt).build());
        return Stream.concat(othermods, artifacts);
    }

    @Override
    public List<ModFileOrException> provide() {
        final var launchHandler = FMLLoader.getLaunchHandler();
        var baseMC = launchHandler.getMinecraftPaths();
        var mcJarContents = new JarContentsBuilder()
                .paths(baseMC.minecraftPaths().toArray(Path[]::new))
                .pathFilter(baseMC.minecraftFilter())
                .build();
        var mcJarMetadata = new ModJarMetadata(mcJarContents);
        var mcSecureJar = SecureJar.from(mcJarContents, mcJarMetadata);
        var mcjar = ModFileFactory.FACTORY.build(mcSecureJar, this, this::buildMinecraftTOML);
        mcJarMetadata.setModFile(mcjar);

        return List.of(new ModFileOrException(mcjar, null));
    }

    private IModFileInfo buildMinecraftTOML(final IModFile iModFile) {
        final ModFile modFile = (ModFile) iModFile;
        /*
        final Path mcmodtoml = modFile.findResource("META-INF", "minecraftmod.toml");
        if (Files.notExists(mcmodtoml)) {
            LOGGER.fatal(LOADING, "Mod file {} is missing minecraftmod.toml file", modFile.getFilePath());
            return null;
        }
        
        final FileConfig mcmodstomlfile = FileConfig.builder(mcmodtoml).build();
        mcmodstomlfile.load();
        mcmodstomlfile.close();
        */

        // We haven't changed this in years, and I can't be asked right now to special case this one file in the path.
        final var conf = Config.inMemory();
        conf.set("modLoader", "minecraft");
        conf.set("loaderVersion", "1");
        conf.set("license", "Mojang Studios, All Rights Reserved");
        final var mods = Config.inMemory();
        mods.set("modId", "minecraft");
        mods.set("version", FMLLoader.versionInfo().mcVersion());
        mods.set("displayName", "Minecraft");
        mods.set("logoFile", "mcplogo.png");
        mods.set("credits", "Mojang, deobfuscated by MCP");
        mods.set("authors", "MCP: Searge,ProfMobius,IngisKahn,Fesh0r,ZeuX,R4wk,LexManos,Bspkrs");
        mods.set("description", "Minecraft, decompiled and deobfuscated with MCP technology");
        conf.set("mods", List.of(mods));
        /*
        conf.putAll(mcmodstomlfile);
        
        final var extralangs = Stream.<IModFileInfo.LanguageSpec>builder();
        final Path forgemodtoml = modFile.findResource("META-INF", "mods.toml");
        if (Files.notExists(forgemodtoml)) {
            LOGGER.info("No forge mods.toml file found, not loading forge mod");
        } else {
            final FileConfig forgemodstomlfile = FileConfig.builder(forgemodtoml).build();
            forgemodstomlfile.load();
            forgemodstomlfile.close();
            conf.putAll(forgemodstomlfile);
            conf.<List<Object>>get("mods").add(0, mcmodstomlfile.<List<Object>>get("mods").get(0)); // Add MC as a sub-mod
            extralangs.add(new IModFileInfo.LanguageSpec(mcmodstomlfile.get("modLoader"), MavenVersionAdapter.createFromVersionSpec(mcmodstomlfile.get("loaderVersion"))));
        }
        */

        final NightConfigWrapper configWrapper = new NightConfigWrapper(conf);
        //final ModFileInfo modFileInfo = new ModFileInfo(modFile, configWrapper, extralangs.build().toList());
        return new ModFileInfo(modFile, configWrapper, configWrapper::setFile, List.of());
    }

    @Override
    public String name() {
        return "minecraft";
    }

    @Override
    public void initArguments(final Map<String, ?> arguments) {
        // no op
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
    }
}
