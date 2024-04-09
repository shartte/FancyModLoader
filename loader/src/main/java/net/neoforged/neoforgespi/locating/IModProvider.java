/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.SecureJar;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

/**
 * Describes objects which can provide mods (or related jars) to the loading runtime.
 */
public interface IModProvider {
    /**
     * The name of the provider.
     * Has to be unique between all providers loaded into the runtime.
     *
     * @return The name.
     */
    String name();

    /**
     * Invoked to scan a particular {@link IModFile} for metadata.
     *
     * @param file         The mod file to scan.
     * @param pathConsumer A consumer which extracts metadata from the path given.
     */
    default void scanFile(IModFile file, Consumer<Path> pathConsumer) {
        final Function<Path, SecureJar.Status> status = p -> file.getSecureJar().verifyPath(p);
        try (Stream<Path> files = Files.find(file.getSecureJar().getRootPath(), Integer.MAX_VALUE, (p, a) -> p.getNameCount() > 0 && p.getFileName().toString().endsWith(".class"))) {
            file.setSecurityStatus(files.peek(pathConsumer).map(status).reduce((s1, s2) -> SecureJar.Status.values()[Math.min(s1.ordinal(), s2.ordinal())]).orElse(SecureJar.Status.INVALID));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Indicates if the given mod file is valid.
     *
     * @param modFile The mod file in question.
     * @return True to mark as valid, false otherwise.
     */
    boolean isValid(IModFile modFile);

    /**
     * A simple record which contains either a valid modfile or a reason one failed to be constructed by {@link #scanMods()}
     * 
     * @param file the file
     * @param ex   an exception that occurred during the attempt to load the mod
     */
    record ModFileOrException(IModFile file, ModFileLoadingException ex) {}

    /**
     * Provides a mod from the given {@code jar}.
     * 
     * @param jar the mod jar contents
     * @return {@code null} if this provider can't provide a mod from that jar, or {@link ModFileOrException} otherwise
     */
    @Nullable
    ModFileOrException provide(JarContents jar);

    /**
     * {@return a list of standalone mods that this provider can provide without being found by a locator}
     */
    default List<ModFileOrException> provide() {
        return List.of();
    }
}
