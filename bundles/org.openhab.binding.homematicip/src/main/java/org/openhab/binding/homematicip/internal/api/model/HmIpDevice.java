package org.openhab.binding.homematicip.internal.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class HmIpDevice {
    // Yes, there is a typ in that constant, the API returns it wrong..
    public static final String TYPE_PLUGGABLE_SWITCH = "PLUGABLE_SWITCH";
    public static final String TYPE_TEMPERATURE_HUMIDITY_SENSOR_DISPLAY = "TEMPERATURE_HUMIDITY_SENSOR_DISPLAY";
    public static final String TYPE_WALL_MOUNTED_THERMOSTAT_PRO = "WALL_MOUNTED_THERMOSTAT_PRO";

    public String id;
    public String homeId;

    public String label;

    public long lastStatusUpdate;

    /**
     * See type constants
     */
    public String type;

    public int manufacturerCode;

    /**
     * Manufacturer
     */
    public String oem;

    /**
     * Model type id
     */
    public long modelId;
    /**
     * Manufacturer model type
     */
    public String modelType;

    public String firmwareVersion;

    public String connectionType;

    public boolean permanentlyReachable;

    /**
     * Each devices has at least one channel.
     * The channel "0" is always the meta channel for the device itself.
     * The channel >= "1" are actual actor channels
     */
    public Map<String, Channel> functionalChannels = new HashMap<>();

    /**
     * Returns the RSSI value of the device or -1 if not supported
     */
    public int getRssiValue() {
        Channel meta = functionalChannels.get("0");
        return (int) meta.properties.getOrDefault("rssiDeviceValue", -1);
    }

    public static class Channel {
        public String label;
        public String deviceId;
        public int index;
        public int groupIndex;
        public String functionalChannelType;

        /**
         * IDs of the groups this channel is a member of
         */
        public List<String> groups = new ArrayList<>();
        public Map<String, Object> properties = new HashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getProperties() {
            return properties;
        }

        @JsonAnySetter
        public void setProperty(String name, Object value) {
            properties.put(name, value);
        }

        public boolean isSwitchChannel() {
            return functionalChannelType.equals("SWITCH_CHANNEL");
        }

        public boolean isOn() {
            return (boolean) properties.getOrDefault("on", false);
        }

        public boolean isThermostat() {
            return functionalChannelType.equals("WALL_MOUNTED_THERMOSTAT_PRO_CHANNEL");
        }

        public int getTemperature() {
            Integer humidity = (Integer) properties.get("actualTemperature");
            if (humidity == null) {
                return -1;
            }
            return humidity;
        }

        public int getHumidity() {
            Integer humidity = (Integer) properties.get("humidity");
            if (humidity == null) {
                return -1;
            }
            return humidity;
        }
    }
}
