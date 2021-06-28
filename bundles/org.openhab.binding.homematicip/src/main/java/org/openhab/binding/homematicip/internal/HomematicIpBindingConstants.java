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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link HomematicIpBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author David Schumann - Initial contribution
 */
@NonNullByDefault
public class HomematicIpBindingConstants {

    public static final String BINDING_ID = "homematicip";

    public static final String TYPE_ID_PLUG_SWITCH = "plugSwitch";
    public static final String TYPE_ID_THERMOSTAT = "thermostat";
    public static final String TYPE_ID_WEATHER_REPORT = "weatherReport";

    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public static final ThingTypeUID THING_TYPE_THERMOSTAT = new ThingTypeUID(BINDING_ID, TYPE_ID_THERMOSTAT);
    public static final ThingTypeUID THING_TYPE_PLUG_SWITCH = new ThingTypeUID(BINDING_ID, TYPE_ID_PLUG_SWITCH);
    public static final ThingTypeUID THING_TYPE_WEATHER_REPORT = new ThingTypeUID(BINDING_ID, TYPE_ID_WEATHER_REPORT);

    /**
     * All things (without bridge)
     */
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPE_UIDS = Set.of(THING_TYPE_THERMOSTAT,
            THING_TYPE_PLUG_SWITCH, THING_TYPE_WEATHER_REPORT);

    public static final String CHANNEL_TEMPERATURE = "temperature";
    public static final String CHANNEL_HUMIDITY = "humidity";
    public static final String CHANNEL_SET_TEMPERATURE = "setTemperature";
    public static final String CHANNEL_SWITCH = "switch";

    public static final String CHANNEL_TYPE_OUTLET = "outlet";
    public static final String CHANNEL_TYPE_AIR_HUMIDITY = "airHumidity";
    public static final String CHANNEL_TYPE_AIR_TEMPERATURE = "airTemperature";
    public static final String CHANNEL_TYPE_SET_TEMPERATURE = "airSetTemperature";
    public static final String CHANNEL_TYPE_WIND_SPEED = "windSpeed";
}
