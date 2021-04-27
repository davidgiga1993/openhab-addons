package org.openhab.binding.homematicip.internal.api.model;

public class Home {
    public Weather weather;

    public boolean connected;
    public String currentAPVersion;
    public String availableAPVersion;
    public double dutyCycle;
    public String id;
    public String timeZoneId;

    public static class Weather {
        public double temperature;
        public double minTemperature;
        public double maxTemperature;
        public double humidity;
        public double windSpeed;
        public double windDirection;
        public double vaporAmount;

        public String weatherCondition;
        public String weatherDayTime;
    }
}
