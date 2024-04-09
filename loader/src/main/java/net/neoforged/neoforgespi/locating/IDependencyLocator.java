/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

import cpw.mods.jarhandling.JarContents;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Loaded as a ServiceLoader. Takes mechanisms for locating candidate "mod-dependencies".
 * and transforms them into {@link IModFile} objects.
 */
public interface IDependencyLocator {
    /**
     * {@return the name of this dependency locator}
     */
    String name();

    /**
     * Invoked to find all mod dependencies that this dependency locator can find.
     * It is not guaranteed that all these are loaded into the runtime,
     * as such the result of this method should be seen as a list of candidates to load.
     *
     * @return All found, or discovered, mod files which function as dependencies.
     */
    List<IModFile> scanMods(final Iterable<IModFile> loadedMods, final Function<JarContents, Optional<IModProvider.ModFileOrException>> provider);
}
