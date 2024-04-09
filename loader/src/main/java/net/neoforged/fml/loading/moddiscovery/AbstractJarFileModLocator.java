/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import net.neoforged.neoforgespi.locating.IModLocator;

public abstract class AbstractJarFileModLocator implements IModLocator {
    protected static List<Path> getLegacyClasspath() {
        return Arrays.stream(System.getProperty("legacyClassPath", "").split(File.pathSeparator)).map(Path::of).toList();
    }
}
