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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.homematicip.internal.DiscoveryService;
import org.openhab.binding.homematicip.internal.api.ApiException;
import org.openhab.binding.homematicip.internal.api.HomematicIp;
import org.openhab.binding.homematicip.internal.api.model.HmIpDevice;
import org.openhab.binding.homematicip.internal.api.model.HomeState;
import org.openhab.binding.homematicip.internal.config.HomematicIpBridgeConfiguration;
import org.openhab.binding.homematicip.internal.update.DeviceValueFetcher;
import org.openhab.binding.homematicip.internal.update.UnknownDeviceState;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomematicIpBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(HomematicIpBridgeHandler.class);
    private final HttpClient httpClient;

    private HomematicIpBridgeConfiguration config;
    private HomematicIp homematicIp;
    private ScheduledFuture<?> pollingJob;

    private HomeState state;
    private long lastUpdate;

    public HomematicIpBridgeHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);
        this.httpClient = httpClient;
    }

    public HomeState getState() throws IOException {
        return homematicIp.loadState();
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(DiscoveryService.class);
    }

    @Override
    public void initialize() {
        config = getConfigAs(HomematicIpBridgeConfiguration.class);
        if (config.accessPoint == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No access point id set");
            return;
        }

        homematicIp = new HomematicIp(httpClient, config.accessPoint, config.authToken);
        try {
            homematicIp.lookupUrl();
        } catch (IOException e) {
            if (e instanceof ApiException) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid auth token");
                return;
            }

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }

        updateStatus(ThingStatus.ONLINE);
        pollingJob = scheduler.scheduleWithFixedDelay(this::pollData, 5, config.refreshInterval, TimeUnit.SECONDS);
    }

    public void updateSwitch(Thing plugSwitchHandler, boolean state) throws IOException {
        String hmipId = (String) plugSwitchHandler.getConfiguration().get(DiscoveryService.REPR_ID);
        // Channel index is always 1
        homematicIp.setSwitchState(hmipId, 1, state);
    }

    public void pollData() {
        // Simple caching
        long delta = System.nanoTime() - lastUpdate;
        if (delta >= TimeUnit.SECONDS.toNanos(config.refreshInterval)) {
            logger.info("Fetching new home state from server");
            try {
                state = homematicIp.loadState();
                updateStatus(ThingStatus.ONLINE);
                lastUpdate = System.nanoTime();
            } catch (IOException e) {

                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                return;
            }
        }

        for (Thing thing : getThing().getThings()) {
            if (!thing.isEnabled())
                continue;

            String hmipId = (String) thing.getConfiguration().get(DiscoveryService.REPR_ID);
            if (hmipId == null) {
                // Thing not configured
                continue;
            }

            for (Channel channel : thing.getChannels()) {
                if (!isLinked(channel.getUID())) {
                    continue;
                }
                HmIpDevice device = state.findDeviceById(hmipId);
                HmipThingHandler handler = (HmipThingHandler) thing.getHandler();
                if (handler == null) {
                    logger.error("Thing has no handler: " + thing.getUID());
                    continue;
                }

                if (device == null) {
                    handler.deviceIdNotFound();
                    continue;
                }

                if (channel.getChannelTypeUID() == null) {
                    // Something went horribly wrong
                    logger.error("Channel type id not defined for channel " + channel.getUID());
                    continue;
                }

                State type;
                try {
                    type = new DeviceValueFetcher(device, channel).convertToType();
                } catch (UnknownDeviceState unknownDeviceState) {
                    handler.unknownDeviceState(unknownDeviceState.getMessage());
                    continue;
                }

                updateState(channel.getUID(), type);
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Not needed
        logger.debug("Bridge command: " + command);
    }

    @Override
    public void dispose() {
        super.dispose();
        final ScheduledFuture<?> job = pollingJob;
        if (job != null) {
            job.cancel(true);
            pollingJob = null;
        }
    }
}
