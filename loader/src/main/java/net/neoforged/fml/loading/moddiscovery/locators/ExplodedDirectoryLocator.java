/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.JarContentsBuilder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.neoforged.neoforgespi.locating.IModLocator;

public class ExplodedDirectoryLocator implements IModLocator {
    public record ExplodedMod(String modid, List<Path> paths) {}

    private final List<ExplodedMod> explodedMods = new ArrayList<>();

    @Override
    public Stream<JarContents> scanCandidates() {
        return explodedMods.stream().map(explodedMod -> {
            return new JarContentsBuilder().paths(explodedMod.paths().toArray(Path[]::new)).build();
        });
    }

    @Override
    public String name() {
        return "exploded directory";
    }

    @Override
    public String toString() {
        return "{ExplodedDir locator}";
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initArguments(final Map<String, ?> arguments) {
        final var explodedTargets = ((Map<String, List<ExplodedMod>>) arguments).get("explodedTargets");
        if (explodedTargets != null && !explodedTargets.isEmpty()) {
            explodedMods.addAll(explodedTargets);
        }
    }
}
