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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;

/**
 * The {@link AirHumidityHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author David Schumann - Initial contribution
 */
@NonNullByDefault
public class AirHumidityHandler extends HmipThingHandler {
    public AirHumidityHandler(Thing thing) {
        super(thing);
    }
}
