package org.openhab.binding.homematicip.internal.api.model;

import org.openhab.binding.homematicip.internal.update.UnknownDeviceState;

public class Home {
    public Weather weather;

    public boolean connected;
    public String currentAPVersion;
    public String availableAPVersion;
    public double dutyCycle;
    public String id;
    public String timeZoneId;

    public static class Weather implements HmIpValues {
        public double temperature;
        public double minTemperature;
        public double maxTemperature;
        public double humidity;
        public double windSpeed;
        public double windDirection;
        public double vaporAmount;

        public String weatherCondition;
        public String weatherDayTime;

        @Override
        public Object getValue(int channel, String key) throws UnknownDeviceState {
            if (HmIpConstants.ChannelProperties.ACTUAL_TEMPERATURE.equals(key)) {
                return temperature;
            }
            if (HmIpConstants.ChannelProperties.HUMIDITY.equals(key)) {
                return humidity;
            }
            if (HmIpConstants.ChannelProperties.WIND_SPEED.equals(key)) {
                return windSpeed;
            }
            throw new UnknownDeviceState("Unknown channel property: " + key);
        }
    }
}
