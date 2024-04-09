/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import cpw.mods.jarhandling.JarContents;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.util.ServiceLoaderUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.neoforged.fml.loading.EarlyLoadingException;
import net.neoforged.fml.loading.ImmediateWindowHandler;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.UniqueModListBuilder;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforgespi.Environment;
import net.neoforged.neoforgespi.locating.IDependencyLocator;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModLocator;
import net.neoforged.neoforgespi.locating.IModProvider;
import net.neoforged.neoforgespi.locating.InvalidModFileException;
import org.slf4j.Logger;

public class ModDiscoverer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ServiceLoader<IModLocator> modLocators;
    private final ServiceLoader<IModProvider> modProviders;
    private final ServiceLoader<IDependencyLocator> dependencyLocators;
    private final List<IModLocator> modLocatorList;
    private final List<IDependencyLocator> dependencyLocatorList;
    private final List<IModProvider> modProviderList;
    private final Function<JarContents, Optional<IModProvider.ModFileOrException>> provider;

    public ModDiscoverer(Map<String, ?> arguments) {
        Launcher.INSTANCE.environment().computePropertyIfAbsent(Environment.Keys.MODDIRECTORYFACTORY.get(), v -> ModsFolderLocator::new);
        Launcher.INSTANCE.environment().computePropertyIfAbsent(Environment.Keys.PROGRESSMESSAGE.get(), v -> StartupNotificationManager.locatorConsumer().orElseGet(() -> s -> {}));
        final var moduleLayerManager = Launcher.INSTANCE.environment().findModuleLayerManager().orElseThrow();
        modLocators = ServiceLoader.load(moduleLayerManager.getLayer(IModuleLayerManager.Layer.SERVICE).orElseThrow(), IModLocator.class);
        modProviders = ServiceLoader.load(moduleLayerManager.getLayer(IModuleLayerManager.Layer.SERVICE).orElseThrow(), IModProvider.class);
        dependencyLocators = ServiceLoader.load(moduleLayerManager.getLayer(IModuleLayerManager.Layer.SERVICE).orElseThrow(), IDependencyLocator.class);
        modLocatorList = ServiceLoaderUtils.streamServiceLoader(() -> modLocators, sce -> LOGGER.error("Failed to load mod locator list", sce)).collect(Collectors.toList());
        modLocatorList.forEach(l -> l.initArguments(arguments));
        modProviderList = ServiceLoaderUtils.streamServiceLoader(() -> modProviders, sce -> LOGGER.error("Failed to load mod provider list", sce)).collect(Collectors.toList());
        dependencyLocatorList = ServiceLoaderUtils.streamServiceLoader(() -> dependencyLocators, sce -> LOGGER.error("Failed to load dependency locator list", sce)).collect(Collectors.toList());
        if (LOGGER.isDebugEnabled(LogMarkers.CORE)) {
            LOGGER.debug(LogMarkers.CORE, "Found Mod Locators : {}", modLocatorList.stream()
                    .map(modLocator -> "(%s:%s)".formatted(modLocator.name(),
                            modLocator.getClass().getPackage().getImplementationVersion()))
                    .collect(Collectors.joining(",")));
        }
        if (LOGGER.isDebugEnabled(LogMarkers.CORE)) {
            LOGGER.debug(LogMarkers.CORE, "Found Dependency Locators : {}", dependencyLocatorList.stream()
                    .map(dependencyLocator -> "(%s:%s)".formatted(dependencyLocator.name(),
                            dependencyLocator.getClass().getPackage().getImplementationVersion()))
                    .collect(Collectors.joining(",")));
        }
        provider = jar -> {
            for (final var prov : modProviderList) {
                final var provided = prov.provide(jar);
                if (provided != null) {
                    return Optional.of(provided);
                }
            }
            return Optional.empty();
        };
    }

    public ModValidator discoverMods() {
        LOGGER.debug(LogMarkers.SCAN, "Scanning for mods and other resources to load. We know {} ways to find mods", modLocatorList.size());
        List<ModFile> loadedFiles = new ArrayList<>();
        List<EarlyLoadingException.ExceptionData> discoveryErrorData = new ArrayList<>();
        List<EarlyLoadingException.ExceptionData> discoveryWarnings = new ArrayList<>();
        boolean successfullyLoadedMods = true;
        ImmediateWindowHandler.updateProgress("Discovering mod files");
        //Loop all mod locators to get the prime mods to load from.
        List<JarContents> candidates = new ArrayList<>();
        for (IModLocator locator : modLocatorList) {
            LOGGER.debug(LogMarkers.SCAN, "Trying locator {}", locator);
            var cands = locator.scanCandidates().toList();
            candidates.addAll(cands);
            LOGGER.debug(LogMarkers.SCAN, "Locator {} found {} candidates", locator, cands.size());
        }

        var unclaimed = new ArrayList<JarContents>();
        // todo - logging
        try {
            var provided = new ArrayList<IModProvider.ModFileOrException>();
            for (JarContents candidate : candidates) {
                final var prov = provider.apply(candidate);
                prov.ifPresentOrElse(provided::add, () -> unclaimed.add(candidate));
            }
            var exceptions = provided.stream().map(IModProvider.ModFileOrException::ex).filter(Objects::nonNull).toList();
            if (!exceptions.isEmpty()) {
                exceptions.stream().map(e -> new EarlyLoadingException.ExceptionData(e.getMessage(), e instanceof InvalidModFileException ime ? ime.getBrokenFile() : null)).forEach(discoveryErrorData::add);
            }
            var locatedFiles = provided.stream().map(IModProvider.ModFileOrException::file).filter(Objects::nonNull).collect(Collectors.toList());

            var badModFiles = locatedFiles.stream().filter(file -> !(file instanceof ModFile)).toList();
            if (!badModFiles.isEmpty()) {
                exceptions.stream().map(e -> new EarlyLoadingException.ExceptionData(e.getMessage(), e instanceof InvalidModFileException ime ? ime.getBrokenFile() : null)).forEach(discoveryErrorData::add);
            }
            locatedFiles.removeAll(badModFiles);
            handleLocatedFiles(loadedFiles, locatedFiles);
        } catch (InvalidModFileException imfe) {
            // We don't generally expect this exception, since it should come from the candidates stream above and be handled in the Locator, but just in case.
            discoveryErrorData.add(new EarlyLoadingException.ExceptionData(imfe.getMessage(), imfe.getBrokenFile()));
        } catch (EarlyLoadingException exception) {
            discoveryErrorData.addAll(exception.getAllData());
        }

        //First processing run of the mod list. Any duplicates will cause resolution failure and dependency loading will be skipped.
        Map<IModFile.Type, List<ModFile>> modFilesMap = Maps.newHashMap();
        try {
            final UniqueModListBuilder modsUniqueListBuilder = new UniqueModListBuilder(loadedFiles);
            final UniqueModListBuilder.UniqueModListData uniqueModsData = modsUniqueListBuilder.buildUniqueList();

            //Grab the temporary results.
            //This allows loading to continue to a base state, in case dependency loading fails.
            modFilesMap = uniqueModsData.modFiles().stream()
                    .collect(Collectors.groupingBy(IModFile::getType));
            loadedFiles = uniqueModsData.modFiles();
        } catch (EarlyLoadingException exception) {
            LOGGER.error(LogMarkers.SCAN, "Failed to build unique mod list after mod discovery.", exception);
            discoveryErrorData.addAll(exception.getAllData());
            successfullyLoadedMods = false;
        }

        //We can continue loading if prime mods loaded successfully.
        if (successfullyLoadedMods) {
            LOGGER.debug(LogMarkers.SCAN, "Successfully Loaded {} mods. Attempting to load dependencies...", loadedFiles.size());
            for (IDependencyLocator locator : dependencyLocatorList) {
                try {
                    LOGGER.debug(LogMarkers.SCAN, "Trying locator {}", locator);
                    final List<IModFile> locatedMods = ImmutableList.copyOf(loadedFiles);

                    var locatedFiles = locator.scanMods(locatedMods, provider);
                    if (locatedFiles.stream().anyMatch(file -> !(file instanceof ModFile))) {
                        LOGGER.error(LogMarkers.SCAN, "A dependency locator returned a file which is not a ModFile instance!. They will be skipped!");
                    }

                    handleLocatedFiles(loadedFiles, locatedFiles);
                } catch (EarlyLoadingException exception) {
                    LOGGER.error(LogMarkers.SCAN, "Failed to load dependencies with locator {}", locator, exception);
                    discoveryErrorData.addAll(exception.getAllData());
                }
            }

            //Second processing run of the mod list. Any duplicates will cause resolution failure and only the mods list will be loaded.
            try {
                final UniqueModListBuilder modsAndDependenciesUniqueListBuilder = new UniqueModListBuilder(loadedFiles);
                final UniqueModListBuilder.UniqueModListData uniqueModsAndDependenciesData = modsAndDependenciesUniqueListBuilder.buildUniqueList();

                //We now only need the mod files map, not the list.
                modFilesMap = uniqueModsAndDependenciesData.modFiles().stream()
                        .collect(Collectors.groupingBy(IModFile::getType));
            } catch (EarlyLoadingException exception) {
                LOGGER.error(LogMarkers.SCAN, "Failed to build unique mod list after dependency discovery.", exception);
                discoveryErrorData.addAll(exception.getAllData());
                modFilesMap = loadedFiles.stream().collect(Collectors.groupingBy(IModFile::getType));
            }
        } else {
            //Failure notify the listeners.
            LOGGER.error(LogMarkers.SCAN, "Mod Discovery failed. Skipping dependency discovery.");
        }

        unclaimed.forEach(jarContents -> {
            var reason = InvalidModIdentifier.identifyJarProblem(jarContents);
            if (reason.isPresent()) {
                LOGGER.warn(LogMarkers.SCAN, "Found jar {} for loader {}. Skipping.", jarContents.getPrimaryPath(), reason.get());
                discoveryWarnings.add(new EarlyLoadingException.ExceptionData(reason.get().getReason(), jarContents.getPrimaryPath()));
            }
        });

        //Validate the loading. With a deduplicated list, we can now successfully process the artifacts and load
        //transformer plugins.
        var validator = new ModValidator(modFilesMap, discoveryWarnings, discoveryErrorData);
        validator.stage1Validation();
        return validator;
    }

    private void handleLocatedFiles(final List<ModFile> loadedFiles, final List<IModFile> locatedFiles) {
        var locatedModFiles = locatedFiles.stream().filter(ModFile.class::isInstance).map(ModFile.class::cast).toList();
        for (IModFile mf : locatedModFiles) {
            LOGGER.info(LogMarkers.SCAN, "Found mod file \"{}\" of type {} with provider {}", mf.getFileName(), mf.getType(), mf.getProvider());
        }
        loadedFiles.addAll(locatedModFiles);
    }
}
