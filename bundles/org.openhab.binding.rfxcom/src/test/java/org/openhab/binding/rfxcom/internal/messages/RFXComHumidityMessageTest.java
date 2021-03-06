/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.rfxcom.internal.messages;

import static org.junit.Assert.assertEquals;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Test;
import org.openhab.binding.rfxcom.internal.exceptions.RFXComException;
import org.openhab.core.util.HexUtils;

/**
 * Test for RFXCom-binding
 *
 * @author Martin van Wingerden - Initial contribution
 */
@NonNullByDefault
public class RFXComHumidityMessageTest {

    @Test
    public void testSomeMessages() throws RFXComException {
        String hexMessage = "085101027700360189";
        byte[] message = HexUtils.hexToBytes(hexMessage);
        RFXComHumidityMessage msg = (RFXComHumidityMessage) RFXComMessageFactory.createMessage(message);
        assertEquals("SubType", RFXComHumidityMessage.SubType.HUM1, msg.subType);
        assertEquals("Seq Number", 2, (short) (msg.seqNbr & 0xFF));
        assertEquals("Sensor Id", "30464", msg.getDeviceId());
        assertEquals("Humidity", 54, msg.humidity);
        assertEquals("Humidity status", RFXComHumidityMessage.HumidityStatus.COMFORT, msg.humidityStatus);
        assertEquals("Signal Level", (byte) 8, msg.signalLevel);
        assertEquals("Battery Level", (byte) 9, msg.batteryLevel);

        byte[] decoded = msg.decodeMessage();

        assertEquals("Message converted back", hexMessage, HexUtils.bytesToHex(decoded));
    }
}
