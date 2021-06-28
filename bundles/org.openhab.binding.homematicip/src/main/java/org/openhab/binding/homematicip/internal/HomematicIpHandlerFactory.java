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
import static org.openhab.binding.homematicip.internal.HomematicIpBindingConstants.THING_TYPE_BRIDGE;
import static org.openhab.binding.homematicip.internal.HomematicIpBindingConstants.THING_TYPE_PLUG_SWITCH;
import static org.openhab.binding.homematicip.internal.HomematicIpBindingConstants.THING_TYPE_THERMOSTAT;
import static org.openhab.binding.homematicip.internal.HomematicIpBindingConstants.THING_TYPE_WEATHER_REPORT;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.homematicip.internal.handler.HomematicIpBridgeHandler;
import org.openhab.binding.homematicip.internal.handler.PlugSwitchHandler;
import org.openhab.binding.homematicip.internal.handler.ThermostatHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link HomematicIpHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author David Schumann - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.homematicip", service = ThingHandlerFactory.class)
public class HomematicIpHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Stream
            .of(Set.of(THING_TYPE_BRIDGE), SUPPORTED_THING_TYPE_UIDS).flatMap(Set::stream).collect(Collectors.toSet());

    private final HttpClient httpClient;

    @Activate
    public HomematicIpHandlerFactory(@Reference HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            return new HomematicIpBridgeHandler((Bridge) thing, httpClient);
        }

        if (THING_TYPE_THERMOSTAT.equals(thingTypeUID)) {
            return new ThermostatHandler(thing);
        }
        if (THING_TYPE_PLUG_SWITCH.equals(thingTypeUID)) {
            return new PlugSwitchHandler(thing);
        }
        if (THING_TYPE_WEATHER_REPORT.equals(thingTypeUID)) {
            return new ThermostatHandler(thing);
        }

        return null;
    }
}
