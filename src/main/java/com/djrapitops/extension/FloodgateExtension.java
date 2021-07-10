/*
 * Copyright(c) 2020 Risto Lahtela (AuroraLS3)
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

package com.djrapitops.extension;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.annotation.BooleanProvider;
import com.djrapitops.plan.extension.annotation.Conditional;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.annotation.StringProvider;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.util.DeviceOs;

import java.util.UUID;

/**
 * The DataExtension for Floodgate.
 *
 * @author Vankka
 */
@PluginInfo(name = "Floodgate", iconName = "gamepad", iconFamily = Family.SOLID, color = Color.AMBER)
public class FloodgateExtension implements DataExtension {

    private final FloodgateStorage storage;

    public FloodgateExtension(FloodgateStorage storage) {
        this.storage = storage;
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[0];
    }

    private boolean isDataAvailable(UUID playerUUID) {
        return storage.getBedrockUsername(playerUUID) != null;
    }

    // Conditionals

    @BooleanProvider(
            text = "Data available",
            priority = 102,
            conditionName = "data-available",
            hidden = true
    )
    public boolean dataAvailable(UUID playerUUID) {
        return isDataAvailable(playerUUID);
    }

    @BooleanProvider(
            text = "Has linked java account",
            priority = 101,
            conditionName = "has-linked-java-account",
            hidden = true
    )
    @Conditional("data-available")
    public boolean hasLinked(UUID playerUUID) {
        return storage.getLinkedPlayer(playerUUID) != null;
    }

    // Player data

    @BooleanProvider(
            text = "Bedrock player",
            description = "If the player is bedrock",
            priority = 100,
            iconName = "gamepad",
            iconColor = Color.AMBER,
            iconFamily = Family.SOLID
    )
    public boolean bedrock(UUID playerUUID) {
        return isDataAvailable(playerUUID) || FloodgateApi.getInstance().isFloodgateId(playerUUID);
    }

    @StringProvider(
            text = "Player device",
            description = "The (last known) device the player is using",
            priority = 90,
            iconName = "mobile-alt",
            iconColor = Color.AMBER,
            iconFamily = Family.REGULAR
    )
    @Conditional("data-available")
    public String device(UUID playerUUID) {
        DeviceOs deviceOS = storage.getPlatform(playerUUID);
        if (deviceOS == null) deviceOS = DeviceOs.UNKNOWN;
        return deviceOS.toString();
    }

    @StringProvider(
            text = "Bedrock username",
            description = "The (last known) bedrock username for the player",
            priority = 80,
            iconName = "signature",
            iconColor = Color.AMBER,
            iconFamily = Family.SOLID
    )
    @Conditional("data-available")
    public String bedrockUsername(UUID playerUUID) {
        return storage.getBedrockUsername(playerUUID);
    }

    @StringProvider(
            text = "Java username",
            description = "The (last known) java username (the prefix & java compliant name from the bedrock name) for the player",
            priority = 70,
            iconName = "file-signature",
            iconColor = Color.AMBER,
            iconFamily = Family.SOLID
    )
    @Conditional("data-available")
    public String javaUsername(UUID playerUUID) {
        return storage.getJavaUsername(playerUUID);
    }

    @StringProvider(
            text = "Linked player",
            description = "The (last known) linked Java player of this bedrock player",
            priority = 60,
            iconName = "link",
            iconColor = Color.AMBER,
            iconFamily = Family.SOLID,
            playerName = true
    )
    @Conditional("has-linked-java-account")
    public String linkedPlayer(UUID playerUUID) {
        return storage.getLinkedPlayer(playerUUID);
    }

    @StringProvider(
            text = "Language code",
            description = "The (last known) language code for the player",
            priority = 50,
            iconName = "flag",
            iconColor = Color.AMBER,
            iconFamily = Family.REGULAR
    )
    @Conditional("data-available")
    public String languageCode(UUID playerUUID) {
        return storage.getLanguageCode(playerUUID);
    }

    @StringProvider(
            text = "Version",
            description = "The (last known) client version for the player",
            priority = 40,
            iconName = "signal",
            iconColor = Color.AMBER,
            iconFamily = Family.SOLID
    )
    @Conditional("data-available")
    public String version(UUID playerUUID) {
        return storage.getVersion(playerUUID);
    }

}
