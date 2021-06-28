package org.openhab.binding.homematicip.internal.update;

import org.openhab.binding.homematicip.internal.HomematicIpBindingConstants;
import org.openhab.binding.homematicip.internal.api.model.HmIpConstants;
import org.openhab.binding.homematicip.internal.api.model.HmIpValues;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Channel;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts the hmip value into a value type for OH
 */
public class ValueFetcher {
    private final HmIpValues device;
    private final Channel channel;

    private final Logger log = LoggerFactory.getLogger(ValueFetcher.class);

    public ValueFetcher(HmIpValues device, Channel channel) {
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
            Boolean value = getValue(1, HmIpConstants.ChannelProperties.ON, Boolean.class);
            return OnOffType.from(value);
        }
        if (HomematicIpBindingConstants.CHANNEL_TYPE_AIR_TEMPERATURE.equals(chTypeId)) {
            double value = getValue(1, HmIpConstants.ChannelProperties.ACTUAL_TEMPERATURE, Double.class);
            return new DecimalType(value);
        }
        if (HomematicIpBindingConstants.CHANNEL_TYPE_AIR_HUMIDITY.equals(chTypeId)) {
            double value = getValue(1, HmIpConstants.ChannelProperties.HUMIDITY, Double.class);
            return new DecimalType(value);
        }
        if (HomematicIpBindingConstants.CHANNEL_TYPE_SET_TEMPERATURE.equals(chTypeId)) {
            double value = getValue(1, HmIpConstants.ChannelProperties.SET_POINT_TEMPERATURE, Double.class);
            return new DecimalType(value);
        }
        if (HomematicIpBindingConstants.CHANNEL_TYPE_WIND_SPEED.equals(chTypeId)) {
            double value = getValue(1, HmIpConstants.ChannelProperties.WIND_SPEED, Double.class);
            return new DecimalType(value);
        }

        log.error("Unknown channel type id: " + chTypeId);
        return null;
    }

    private <T> T getValue(int channelId, String key, Class<T> target) throws UnknownDeviceState {
        Object value = device.getValue(channelId, key);
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
