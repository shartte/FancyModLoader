/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.JarContentsBuilder;
import cpw.mods.jarhandling.SecureJar;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.neoforged.fml.loading.EarlyLoadingException;
import net.neoforged.jarjar.selection.JarSelector;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IDependencyLocator;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModProvider;
import net.neoforged.neoforgespi.locating.ModFileFactory;
import net.neoforged.neoforgespi.locating.ModFileLoadingException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class JarInJarDependencyLocator implements IDependencyLocator, IModProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String name() {
        return "jarinjar";
    }

    @Override
    public void scanFile(IModFile modFile, Consumer<Path> pathConsumer) {}

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
    }

    @Override
    public @Nullable ModFileOrException provide(JarContents jar) {
        return null;
    }

    @Override
    public List<IModFile> scanMods(Iterable<IModFile> loadedMods, Function<JarContents, Optional<ModFileOrException>> provider) {
        final List<IModFile> sources = Lists.newArrayList();
        loadedMods.forEach(sources::add);

        final List<IModFile> dependenciesToLoad = JarSelector.detectAndSelect(sources, this::loadResourceFromModFile, this::loadModFileFrom, this::identifyMod, this::exception);

        if (dependenciesToLoad.isEmpty()) {
            LOGGER.info("No dependencies to load found. Skipping!");
            return Collections.emptyList();
        }

        LOGGER.info("Found {} dependencies adding them to mods collection", dependenciesToLoad.size());
        return dependenciesToLoad;
    }

    @SuppressWarnings("resource")
    @Override
    protected Optional<IModFile> loadModFileFrom(final IModFile file, final Path path, Function<JarContents, Optional<IModProvider.ModFileOrException>> provider) {
        try {
            final Path pathInModFile = file.findResource(path.toString());
            final URI filePathUri = new URI("jij:" + (pathInModFile.toAbsolutePath().toUri().getRawSchemeSpecificPart())).normalize();
            final Map<String, ?> outerFsArgs = ImmutableMap.of("packagePath", pathInModFile);
            final FileSystem zipFS = FileSystems.newFileSystem(filePathUri, outerFsArgs);
            final var jar = new JarContentsBuilder().paths(zipFS.getPath("/")).build();
            final var fileOrEx = provider.apply(jar)
                    .orElseGet(() -> new ModFileOrException(ModFileFactory.FACTORY.build(SecureJar.from(jar), this, JarModsDotTomlModProvider::manifestParser, IModFile.Type.LIBRARY), null));
            if (fileOrEx.ex() != null) {
                throw fileOrEx.ex();
            }
            return Optional.of(fileOrEx.file());
        } catch (Exception e) {
            LOGGER.error("Failed to load mod file {} from {}", path, file.getFileName());
            final RuntimeException exception = new ModFileLoadingException("Failed to load mod file " + file.getFileName());
            exception.initCause(e);

            throw exception;
        }
    }

    protected EarlyLoadingException exception(Collection<JarSelector.ResolutionFailureInformation<IModFile>> failedDependencies) {
        final List<EarlyLoadingException.ExceptionData> errors = failedDependencies.stream()
                .filter(entry -> !entry.sources().isEmpty()) //Should never be the case, but just to be sure
                .map(this::buildExceptionData)
                .toList();

        return new EarlyLoadingException(failedDependencies.size() + " Dependency restrictions were not met.", null, errors);
    }

    private EarlyLoadingException.ExceptionData buildExceptionData(final JarSelector.ResolutionFailureInformation<IModFile> entry) {
        return new EarlyLoadingException.ExceptionData(
                getErrorTranslationKey(entry),
                entry.identifier().group() + ":" + entry.identifier().artifact(),
                entry.sources()
                        .stream()
                        .flatMap(this::getModWithVersionRangeStream)
                        .map(this::formatError)
                        .collect(Collectors.joining(", ")));
    }

    private String getErrorTranslationKey(final JarSelector.ResolutionFailureInformation<IModFile> entry) {
        return entry.failureReason() == JarSelector.FailureReason.VERSION_RESOLUTION_FAILED ? "fml.dependencyloading.conflictingdependencies" : "fml.dependencyloading.mismatchedcontaineddependencies";
    }

    private Stream<ModWithVersionRange> getModWithVersionRangeStream(final JarSelector.SourceWithRequestedVersionRange<IModFile> file) {
        return file.sources()
                .stream()
                .map(IModFile::getModFileInfo)
                .flatMap(modFileInfo -> modFileInfo.getMods().stream())
                .map(modInfo -> new ModWithVersionRange(modInfo, file.requestedVersionRange(), file.includedVersion()));
    }

    private String formatError(final ModWithVersionRange modWithVersionRange) {
        return "\u00a7e" + modWithVersionRange.modInfo().getModId() + "\u00a7r - \u00a74" + modWithVersionRange.versionRange().toString() + "\u00a74 - \u00a72" + modWithVersionRange.artifactVersion().toString() + "\u00a72";
    }

    protected String identifyMod(final IModFile modFile) {
        if (modFile.getModFileInfo() == null || modFile.getModInfos().isEmpty()) {
            return modFile.getFileName();
        }

        return modFile.getModInfos().stream().map(IModInfo::getModId).collect(Collectors.joining());
    }

    private record ModWithVersionRange(IModInfo modInfo, VersionRange versionRange, ArtifactVersion artifactVersion) {}

    protected Optional<InputStream> loadResourceFromModFile(final IModFile modFile, final Path path) {
        try {
            return Optional.of(Files.newInputStream(modFile.findResource(path.toString())));
        } catch (final NoSuchFileException e) {
            LOGGER.trace("Failed to load resource {} from {}, it does not contain dependency information.", path, modFile.getFileName());
            return Optional.empty();
        } catch (final Exception e) {
            LOGGER.error("Failed to load resource {} from mod {}, cause {}", path, modFile.getFileName(), e);
            return Optional.empty();
        }
    }
}
