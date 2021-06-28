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
package org.openhab.binding.homematicip.internal;

import static org.openhab.binding.homematicip.internal.HomematicIpBindingConstants.SUPPORTED_THING_TYPE_UIDS;
import static org.openhab.binding.homematicip.internal.HomematicIpBindingConstants.TYPE_ID_WEATHER_REPORT;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.homematicip.internal.api.model.HmIpDevice;
import org.openhab.binding.homematicip.internal.api.model.HomeState;
import org.openhab.binding.homematicip.internal.handler.HomematicIpBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class DiscoveryService extends AbstractDiscoveryService implements ThingHandlerService {
    private static final int SEARCH_TIME = 10;

    public static final String REPR_ID = "hmipId";

    @Nullable
    private HomematicIpBridgeHandler bridgeHandler;

    private final Logger logger = LoggerFactory.getLogger(DiscoveryService.class);

    public DiscoveryService() throws IllegalArgumentException {
        super(Collections.unmodifiableSet(
                Stream.of(SUPPORTED_THING_TYPE_UIDS).flatMap(Set::stream).collect(Collectors.toSet())), SEARCH_TIME);
        logger.info("Discovery created");
    }

    @Override
    protected void startScan() {
        if (bridgeHandler == null) {
            logger.error("No bridge handler available");
            return;
        }

        HomeState state;
        try {
            state = bridgeHandler.getState();
        } catch (IOException e) {
            logger.error(e.getMessage());
            return;
        }

        if (state.home.weather != null) {
            ThingTypeUID thingTypeUID = new ThingTypeUID(HomematicIpBindingConstants.BINDING_ID,
                    TYPE_ID_WEATHER_REPORT);
            ThingUID thingUID = new ThingUID(thingTypeUID, bridgeHandler.getThing().getUID(), TYPE_ID_WEATHER_REPORT);

            Map<String, Object> properties = new HashMap<>();
            properties.put(REPR_ID, TYPE_ID_WEATHER_REPORT);

            DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withRepresentationProperty(REPR_ID).withBridge(bridgeHandler.getThing().getUID())
                    .withLabel("Weather report").build();
            thingDiscovered(result);
        }

        List<HmIpDevice> switches = state.findDevicesByType(HmIpDevice.TYPE_PLUGGABLE_SWITCH);
        for (HmIpDevice switchDevice : switches) {
            hmIpDeviceDiscovered(switchDevice, HomematicIpBindingConstants.TYPE_ID_PLUG_SWITCH);
        }

        List<HmIpDevice> thermostats = state.findDevicesByType(HmIpDevice.TYPE_TEMPERATURE_HUMIDITY_SENSOR_DISPLAY);
        thermostats.addAll(state.findDevicesByType(HmIpDevice.TYPE_WALL_MOUNTED_THERMOSTAT_PRO));
        for (HmIpDevice thermostat : thermostats) {
            hmIpDeviceDiscovered(thermostat, HomematicIpBindingConstants.TYPE_ID_THERMOSTAT);
        }
    }

    private void hmIpDeviceDiscovered(HmIpDevice switchDevice, String typeIdPlugSwitch) {
        ThingTypeUID thingTypeUID = new ThingTypeUID(HomematicIpBindingConstants.BINDING_ID, typeIdPlugSwitch);
        ThingUID thingUID = new ThingUID(thingTypeUID, bridgeHandler.getThing().getUID(), switchDevice.id);

        Map<String, Object> properties = new HashMap<>();
        properties.put(REPR_ID, switchDevice.id);
        properties.put("modelType", switchDevice.modelType);
        properties.put("firmwareVersion", switchDevice.firmwareVersion);
        properties.put("oem", switchDevice.oem);
        properties.put("permanentlyReachable", switchDevice.permanentlyReachable);
        properties.put("connectionType", switchDevice.connectionType);
        properties.put("lastStatusUpdate", switchDevice.lastStatusUpdate);
        properties.put("type", switchDevice.type);

        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                .withRepresentationProperty(REPR_ID).withBridge(bridgeHandler.getThing().getUID())
                .withLabel(switchDevice.label).build();
        thingDiscovered(result);
    }

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (handler instanceof HomematicIpBridgeHandler) {
            this.bridgeHandler = (HomematicIpBridgeHandler) handler;
            return;
        }
        logger.error("Invalid handler: " + handler);
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
}
