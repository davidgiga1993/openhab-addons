package org.openhab.binding.homematicip.internal.update;

import org.openhab.binding.homematicip.internal.HomematicIpBindingConstants;
import org.openhab.binding.homematicip.internal.api.model.HmIpDevice;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Channel;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts the hmip device value into a value type
 */
public class DeviceValueFetcher {
    private final HmIpDevice device;
    private final Channel channel;

    private final Logger log = LoggerFactory.getLogger(DeviceValueFetcher.class);

    public DeviceValueFetcher(HmIpDevice device, Channel channel) {
        this.device = device;
        this.channel = channel;
    }

    /**
     * Converts the device value for the current channel to a state
     *
     * @return State or null if conversion failed
     */
    public State convertToType() throws UnknownDeviceState {
        String chTypeId = channel.getChannelTypeUID().getId();

        if (HomematicIpBindingConstants.CHANNEL_TYPE_OUTLET.equals(chTypeId)) {
            HmIpDevice.Channel hmipChannel = getChannel(1);
            Boolean value = getValue(hmipChannel, "on", Boolean.class);
            return OnOffType.from(value);
        }
        if (HomematicIpBindingConstants.CHANNEL_TYPE_AIR_TEMPERATURE.equals(chTypeId)) {
            HmIpDevice.Channel hmipChannel = getChannel(1);
            double value = getValue(hmipChannel, "actualTemperature", Double.class);
            return new DecimalType(value);
        }
        if (HomematicIpBindingConstants.CHANNEL_TYPE_AIR_HUMIDITY.equals(chTypeId)) {
            HmIpDevice.Channel hmipChannel = getChannel(1);
            double value = getValue(hmipChannel, "humidity", Double.class);
            return new DecimalType(value);
        }
        if (HomematicIpBindingConstants.CHANNEL_TYPE_SET_TEMPERATURE.equals(chTypeId)) {
            HmIpDevice.Channel hmipChannel = getChannel(1);
            double value = getValue(hmipChannel, "setPointTemperature", Double.class);
            return new DecimalType(value);
        }

        log.error("Unknown channel type id: " + chTypeId);
        return null;
    }

    private HmIpDevice.Channel getChannel(int channelId) throws UnknownDeviceState {
        HmIpDevice.Channel channel = device.functionalChannels.get(String.valueOf(channelId));
        if (channel == null) {
            throw new UnknownDeviceState("Missing functional channel 1 of device " + device.label);
        }
        return channel;
    }

    private <T> T getValue(HmIpDevice.Channel hmipChannel, String key, Class<T> target) throws UnknownDeviceState {
        Object value = hmipChannel.properties.get(key);
        if (value == null) {
            throw new UnknownDeviceState("Unknown device state of device " + device.label);
        }
        if (target == Double.class) {
            // Might be int
            Class<?> type = value.getClass();
            if (type == Integer.class) {
                return (T) (Double) ((Integer) value).doubleValue();
            }
            if (type == Long.class) {
                return (T) (Double) ((Long) value).doubleValue();
            }
        }

        return (T) value;
    }
}
