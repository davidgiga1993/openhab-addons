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

import static org.openhab.binding.homematicip.internal.HomematicIpBindingConstants.CHANNEL_SET_TEMPERATURE;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;

/**
 * The {@link ThermostatHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Schumann - Initial contribution
 */
@NonNullByDefault
public class ThermostatHandler extends HmipThingHandler {
    public ThermostatHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!CHANNEL_SET_TEMPERATURE.equals(channelUID.getId())) {
            // Unsupported channel
            return;
        }
        if (command instanceof DecimalType) {
            double temperature = ((DecimalType) command).doubleValue();

            HomematicIpBridgeHandler bridge = (HomematicIpBridgeHandler) getBridge().getHandler();
            try {
                bridge.updateTemperature(getThing(), temperature);
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                return;
            }

            updateState(CHANNEL_SET_TEMPERATURE, (DecimalType) command);
            return;
        }
        super.handleCommand(channelUID, command);
    }
}
