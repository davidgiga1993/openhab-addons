package org.openhab.binding.homematicip.internal.api.model;

import java.util.List;

public class HmIpGroup {
    public static final String TYPE_HEATING = "HEATING";

    public String id;
    public String homeId;
    public String label;
    public String type;

    public List<GroupChannel> channels;

    public static class GroupChannel {
        public String deviceId;
        public int channelIndex;
    }
}
