/*
 * Minecraft Forge
 * Copyright (c) 2016-2019.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.neoforged.neoforgespi.locating;

import cpw.mods.jarhandling.JarContents;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loaded as a ServiceLoader. Takes mechanisms for locating candidate "mod" JARs.
 */
public interface IModLocator {
    /**
     * {@return all mod paths that this mod locator can find}
     */
    Stream<JarContents> scanCandidates();

    /**
     * {@return the name of this locator}
     */
    String name();

    /**
     * Invoked with the game startup arguments to allow for configuration of the provider.
     *
     * @param arguments The arguments.
     */
    default void initArguments(Map<String, ?> arguments) {}
}
