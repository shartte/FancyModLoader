/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import com.mojang.logging.LogUtils;
import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.JarContentsBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.neoforged.fml.loading.ClasspathLocatorUtils;
import net.neoforged.fml.loading.LogMarkers;
import org.slf4j.Logger;

public class ClasspathLocator extends AbstractJarFileModLocator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<Path> legacyClasspath = AbstractJarFileModLocator.getLegacyClasspath();
    private boolean enabled = false;

    @Override
    public String name() {
        return "userdev classpath";
    }

    @Override
    public Stream<JarContents> scanCandidates() {
        if (!enabled)
            return Stream.of();

        try {
            var claimed = new ArrayList<>(legacyClasspath);
            var paths = Stream.<Path>builder();

            findPaths(claimed, JarModsDotTomlModProvider.MODS_TOML).forEach(paths::add);
            findPaths(claimed, JarModsDotTomlModProvider.MANIFEST).forEach(paths::add);

            return paths.build().map(p -> new JarContentsBuilder().paths(p).build());
        } catch (IOException e) {
            LOGGER.error(LogMarkers.SCAN, "Error trying to find resources", e);
            throw new RuntimeException(e);
        }
    }

    private List<Path> findPaths(List<Path> claimed, String resource) throws IOException {
        var ret = new ArrayList<Path>();
        final Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources(resource);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            Path path = ClasspathLocatorUtils.findJarPathFor(resource, resource, url);
            if (claimed.stream().anyMatch(path::equals) || !Files.exists(path) || Files.isDirectory(path))
                continue;
            ret.add(path);
        }
        return ret;
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
        var launchTarget = (String) arguments.get("launchTarget");
        enabled = launchTarget != null && launchTarget.contains("dev");
    }

    private Path findJarPathFor(final String resourceName, final String jarName, final URL resource) {
        try {
            Path path;
            final URI uri = resource.toURI();
            if (uri.getScheme().equals("jar") && uri.getRawSchemeSpecificPart().contains("!/")) {
                int lastExcl = uri.getRawSchemeSpecificPart().lastIndexOf("!/");
                path = Paths.get(new URI(uri.getRawSchemeSpecificPart().substring(0, lastExcl)));
            } else {
                path = Paths.get(new URI("file://" + uri.getRawSchemeSpecificPart().substring(0, uri.getRawSchemeSpecificPart().length() - resourceName.length())));
            }
            //LOGGER.debug(CORE, "Found JAR {} at path {}", jarName, path.toString());
            return path;
        } catch (NullPointerException | URISyntaxException e) {
            LOGGER.error(LogMarkers.SCAN, "Failed to find JAR for class {} - {}", resourceName, jarName);
            throw new RuntimeException("Unable to locate " + resourceName + " - " + jarName, e);
        }
    }
}
