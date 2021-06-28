package org.openhab.binding.homematicip.internal.api.model;

import org.openhab.binding.homematicip.internal.update.UnknownDeviceState;

public interface HmIpValues {
    /**
     * Returns a value at the given channel and key
     *
     * @param channel Channel
     * @param key Key
     * @return Value
     * @throws UnknownDeviceState Channel or key not known
     */
    Object getValue(int channel, String key) throws UnknownDeviceState;
}
