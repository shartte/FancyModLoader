/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

import org.jetbrains.annotations.Nullable;

/**
 * Attributes of a modfile relating to how it was discovered.
 *
 * @param parent        The mod file that logically contains this mod file.
 * @param reader        The reader that was used to get a mod-file from jar contents. May be null if the mod file was directly created by a locator.
 * @param systemModFile True if this mod file is supplied by the modding system and is searched for basic system mods.
 */
public record ModFileDiscoveryAttributes(@Nullable IModFile parent,
        @Nullable IModFileReader reader,
        @Nullable IModFileCandidateLocator locator,
        @Nullable IDependencyLocator dependencyLocator,
        boolean systemModFile) {

    public static final ModFileDiscoveryAttributes DEFAULT = new ModFileDiscoveryAttributes(null, null, null, null, false);
    public ModFileDiscoveryAttributes withSystemModFile(boolean systemModFile) {
        return new ModFileDiscoveryAttributes(parent, reader, locator, dependencyLocator, systemModFile);
    }

    public ModFileDiscoveryAttributes withParent(IModFile parent) {
        return new ModFileDiscoveryAttributes(parent, reader, locator, dependencyLocator, systemModFile);
    }

    public ModFileDiscoveryAttributes withReader(IModFileReader reader) {
        return new ModFileDiscoveryAttributes(parent, reader, locator, dependencyLocator, systemModFile);
    }

    public ModFileDiscoveryAttributes withLocator(IModFileCandidateLocator locator) {
        return new ModFileDiscoveryAttributes(parent, reader, locator, dependencyLocator, systemModFile);
    }

    public ModFileDiscoveryAttributes withDependencyLocator(IDependencyLocator dependencyLocator) {
        return new ModFileDiscoveryAttributes(parent, reader, locator, dependencyLocator, systemModFile);
    }

    public ModFileDiscoveryAttributes merge(ModFileDiscoveryAttributes attributes) {
        return new ModFileDiscoveryAttributes(
                attributes.parent != null ? attributes.parent : parent,
                attributes.reader != null ? attributes.reader : reader,
                attributes.locator != null ? attributes.locator : locator,
                attributes.dependencyLocator != null ? attributes.dependencyLocator : dependencyLocator,
                attributes.systemModFile || systemModFile);
    }

    @Override
    public String toString() {
        var result = new StringBuilder();
        result.append("[");
        if (parent != null) {
            result.append("parent: ");
            result.append(parent.getFilePath().getFileName());
        }
        if (locator != null) {
            if (result.length() > 1) {
                result.append(", ");
            }
            result.append("locator: ");
            result.append(locator);
        }
        if (dependencyLocator != null) {
            if (result.length() > 1) {
                result.append(", ");
            }
            result.append("locator: ");
            result.append(dependencyLocator);
        }
        if (reader != null) {
            if (result.length() > 1) {
                result.append(", ");
            }
            result.append("reader: ");
            result.append(reader);
        }
        if (systemModFile) {
            if (result.length() > 1) {
                result.append(", ");
            }
            result.append("system");
        }
        result.append("]");
        return result.toString();
    }
}
