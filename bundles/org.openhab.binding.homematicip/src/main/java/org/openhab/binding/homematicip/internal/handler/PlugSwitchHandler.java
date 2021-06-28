/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 * <p>
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.homematicip.internal.handler;

import static org.openhab.binding.homematicip.internal.HomematicIpBindingConstants.CHANNEL_SWITCH;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;

/**
 * The {@link PlugSwitchHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Schumann - Initial contribution
 */
@NonNullByDefault
public class PlugSwitchHandler extends HmipThingHandler {
    public PlugSwitchHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!CHANNEL_SWITCH.equals(channelUID.getId())) {
            // Unsupported channel
            return;
        }
        if (command instanceof OnOffType) {
            boolean state = command == OnOffType.ON;

            HomematicIpBridgeHandler bridge = (HomematicIpBridgeHandler) getBridge().getHandler();
            try {
                bridge.updateSwitch(getThing(), state);
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                return;
            }

            updateState(CHANNEL_SWITCH, (OnOffType) command);
            return;
        }

        super.handleCommand(channelUID, command);
    }
}