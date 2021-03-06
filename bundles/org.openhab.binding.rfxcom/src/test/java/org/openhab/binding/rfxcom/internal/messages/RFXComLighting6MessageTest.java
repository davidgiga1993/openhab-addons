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
public class RFXComLighting6MessageTest {

    @Test
    public void testSomeMessages() throws RFXComException {
        String hexMessage = "0B150005D950450101011D80";
        byte[] message = HexUtils.hexToBytes(hexMessage);
        RFXComLighting6Message msg = (RFXComLighting6Message) RFXComMessageFactory.createMessage(message);
        assertEquals("SubType", RFXComLighting6Message.SubType.BLYSS, msg.subType);
        assertEquals("Seq Number", 5, (short) (msg.seqNbr & 0xFF));
        assertEquals("Sensor Id", "55632.E.1", msg.getDeviceId());
        assertEquals("Command", RFXComLighting6Message.Commands.OFF, msg.command);
        assertEquals("Signal Level", (byte) 8, msg.signalLevel);

        byte[] decoded = msg.decodeMessage();

        // the openhab plugin is not (yet) using the cmndseqnbr & seqnbr2
        String acceptedHexMessage = "0B150005D950450101000080";

        assertEquals("Message converted back", acceptedHexMessage, HexUtils.bytesToHex(decoded));
    }
}
