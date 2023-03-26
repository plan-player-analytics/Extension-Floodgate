package net.playeranalytics.extension.floodgate;

import com.djrapitops.plan.extension.Caller;
import com.djrapitops.plan.settings.ListenerService;
import com.djrapitops.plan.settings.SchedulerService;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.LinkedPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class FloodgateVelocityListener extends FloodgateListener {

    public FloodgateVelocityListener(FloodgateStorage storage, Caller caller) {
        super(storage, caller);
    }

    @Override
    public void register() {
        ListenerService.getInstance().registerListenerForPlan(this);
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onPostLogin(PostLoginEvent event) {
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

    private void storeData(PostLoginEvent event, UUID uuid, FloodgatePlayer floodgatePlayer, LinkedPlayer linkedPlayer) {
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
            caller.updatePlayerData(uuid, event.getPlayer().getUsername());
        } catch (ExecutionException ignored) {
            // Ignore
        }
    }
}
