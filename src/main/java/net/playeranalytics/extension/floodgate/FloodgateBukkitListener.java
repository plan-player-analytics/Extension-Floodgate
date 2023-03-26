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
import com.djrapitops.plan.settings.ListenerService;
import com.djrapitops.plan.settings.SchedulerService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.LinkedPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class FloodgateBukkitListener extends FloodgateListener implements Listener {

    public FloodgateBukkitListener(FloodgateStorage storage, Caller caller) {
        super(storage, caller);
    }

    @Override
    public void register() {
        ListenerService.getInstance().registerListenerForPlan(this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        try {
            UUID uuid = event.getPlayer().getUniqueId();

            Optional.ofNullable(FloodgateApi.getInstance())
                    .map(api -> api.getPlayer(uuid))
                    .ifPresent(floodgatePlayer -> {
                        LinkedPlayer linkedPlayer = floodgatePlayer.getLinkedPlayer();
                        SchedulerService.getInstance()
                                .runAsync(() -> storeData(event, uuid, floodgatePlayer, linkedPlayer));
                    });
        } catch (LinkageError ignored) {
            // Related to
            // https://github.com/plan-player-analytics/Plan/issues/2004
            // https://github.com/GeyserMC/Floodgate/issues/178
        }
    }

    private void storeData(PlayerJoinEvent event, UUID uuid, FloodgatePlayer floodgatePlayer, LinkedPlayer linkedPlayer) {
        try {
            storage.storePlayer(
                    uuid,
                    floodgatePlayer.getDeviceOs(),
                    floodgatePlayer.getUsername(),
                    floodgatePlayer.getJavaUsername(),
                    linkedPlayer != null ? linkedPlayer.getJavaUsername() : null,
                    floodgatePlayer.getLanguageCode(),
                    floodgatePlayer.getVersion()
            );
            caller.updatePlayerData(uuid, event.getPlayer().getName());
        } catch (ExecutionException ignored) {
            // Ignore
        }
    }
}
