/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import cpw.mods.jarhandling.JarContents;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import net.neoforged.fml.loading.StringUtils;

public enum InvalidModIdentifier {
    OLDFORGE(filePresent("mcmod.info")),
    MINECRAFT_FORGE(filePresent("mods.toml")),
    FABRIC(filePresent("fabric.mod.json")),
    LITELOADER(filePresent("litemod.json")),
    OPTIFINE(filePresent("optifine/Installer.class")),
    BUKKIT(filePresent("plugin.yml"));

    private final Predicate<JarContents> ident;

    InvalidModIdentifier(Predicate<JarContents> identifier) {
        this.ident = identifier;
    }

    public String getReason() {
        return "fml.modloading.brokenfile." + StringUtils.toLowerCase(name());
    }

    public static Optional<InvalidModIdentifier> identifyJarProblem(JarContents jar) {
        return Arrays.stream(values())
                .filter(i -> i.ident.test(jar))
                .findAny();
    }

    private static Predicate<JarContents> filePresent(String filename) {
        return jarContents -> jarContents.findFile(filename).isPresent();
    }
}
