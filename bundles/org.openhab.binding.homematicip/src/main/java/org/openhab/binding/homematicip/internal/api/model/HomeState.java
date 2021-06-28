package org.openhab.binding.homematicip.internal.api.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The current state of the entire system
 */
public class HomeState {
    public Home home;

    public Map<String, HmIpDevice> devices = new HashMap<>();
    public Map<String, HmIpGroup> groups = new HashMap<>();

    public List<HmIpDevice> findDevicesByType(String type) {
        return devices.values().stream().filter(d -> type.equals(d.type)).collect(Collectors.toList());
    }

    public HmIpDevice findDeviceById(String id) {
        return devices.get(id);
    }

    public Optional<HmIpGroup> findGroupOfDevice(String groupType, String deviceId) {
        return groups.values().stream().filter(group -> {
            if (!group.type.equals(groupType))
                return false;
            return group.channels.stream().anyMatch(ch -> ch.deviceId.equals(deviceId));
        }).findAny();
    }
}
