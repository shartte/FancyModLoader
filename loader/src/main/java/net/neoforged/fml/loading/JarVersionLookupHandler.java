/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import java.lang.module.ModuleDescriptor;
import java.util.Optional;

/**
 * Finds Version data from a package, with possible default values
 */
public class JarVersionLookupHandler {
    public static Optional<String> getImplementationVersion(final Class<?> clazz) {
        return getOptionalModuleVersion(clazz).map(ModuleDescriptor.Version::toString);
    }

    public static Optional<String> getSpecificationVersion(final Class<?> clazz) {
        return getOptionalModuleVersion(clazz).map(ModuleDescriptor.Version::toString);
    }

    public static Optional<String> getImplementationTitle(final Class<?> clazz) {
        return Optional.empty();
    }

    public static Optional<ModuleDescriptor.Version> getOptionalModuleVersion(final Class<?> clazz) {
        // With java 9 we'll use the module's version if it exists in preference.
        var descriptor = clazz.getModule().getDescriptor();
        return descriptor.version()
                .or(() -> Optional.ofNullable(clazz.getPackage().getImplementationVersion())
                        .map(ModuleDescriptor.Version::parse));
    }

    public static ModuleDescriptor.Version getModuleVersion(final Class<?> clazz) {
        return getOptionalModuleVersion(clazz)
                .orElseThrow(() -> {
                    if (clazz.getModule().isNamed()) {
                        return new IllegalStateException("Could not determine version of module " + clazz.getModule().getName());
                    } else {
                        return new IllegalStateException("Could not determine version of module for class " + clazz);
                    }
                });
    }
}
