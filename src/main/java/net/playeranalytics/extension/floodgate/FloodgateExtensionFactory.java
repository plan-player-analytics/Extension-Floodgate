/*
 * Copyright(c) 2020 AuroraLS3
 *
 * The MIT License(MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files(the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions :
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.playeranalytics.extension.floodgate;

import com.djrapitops.plan.extension.Caller;
import com.djrapitops.plan.extension.DataExtension;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Factory for the FloodgateExtension.
 *
 * @author Vankka
 */
public class FloodgateExtensionFactory {

    private FloodgateStorage storage;
    private BiFunction<FloodgateStorage, Caller, FloodgateListener> listenerConstructor;

    private boolean isAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }

    public Optional<DataExtension> createExtension() {
        FloodgateExtension extension = createNewExtension();
        return Optional.ofNullable(extension);
    }

    public void registerListener(Caller caller) {
        listenerConstructor.apply(storage, caller).register();
        listenerConstructor = null; // we don't need it anymore
    }

    private FloodgateExtension createNewExtension() {
        BiFunction<FloodgateStorage, Caller, FloodgateListener> constructListener = getListenerConstructor();
        // No supported Floodgate plugin
        if (constructListener == null) return null;

        this.storage = new FloodgateStorage();
        this.listenerConstructor = constructListener;
        return new FloodgateExtension(storage);
    }

    private BiFunction<FloodgateStorage, Caller, FloodgateListener> getListenerConstructor() {
        if (isAvailable("org.geysermc.floodgate.SpigotPlugin")) {
            return FloodgateBukkitListener::new;
        } else if (isAvailable("org.geysermc.floodgate.BungeePlugin")) {
            return FloodgateBungeeListener::new;
        } else if (isAvailable("org.geysermc.floodgate.VelocityPlugin")) {
            return FloodgateVelocityListener::new;
        } else {
            return null;
        }
    }

}
