/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import com.electronwill.nightconfig.core.Config;
import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.JarContentsBuilder;
import cpw.mods.jarhandling.SecureJar;
import java.nio.file.Path;
import java.util.List;
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

public class MinecraftLocator implements IModProvider, IModLocator {
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

        final NightConfigWrapper configWrapper = new NightConfigWrapper(conf);
        return new ModFileInfo(modFile, configWrapper, configWrapper::setFile, List.of());
    }

    @Override
    public String name() {
        return "minecraft";
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
    }
}
