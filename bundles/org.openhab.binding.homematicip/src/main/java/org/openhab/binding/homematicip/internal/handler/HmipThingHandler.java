package org.openhab.binding.homematicip.internal.handler;

import org.openhab.binding.homematicip.internal.DiscoveryService;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HmipThingHandler extends BaseThingHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Creates a new instance of this class for the {@link Thing}.
     *
     * @param thing the thing that should be handled, not null
     */
    public HmipThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        super.bridgeStatusChanged(bridgeStatusInfo);
        logger.info("Bridge status changed: " + bridgeStatusInfo.getStatus());
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            // Refresh data
            HomematicIpBridgeHandler bridgeHandler = (HomematicIpBridgeHandler) getBridge().getHandler();
            bridgeHandler.pollData();
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            logger.info("Refreshing data...");
            HomematicIpBridgeHandler bridgeHandler = (HomematicIpBridgeHandler) getBridge().getHandler();
            bridgeHandler.pollData();
            return;
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.ONLINE);
    }

    /**
     * Gets called when the device does not exist anymore on the homematic IP server side
     */
    public void deviceIdNotFound() {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                DiscoveryService.REPR_ID + " not found on backend");
    }

    /**
     * Gets called if the device state of this thing is unknown
     */
    public void unknownDeviceState(String message) {
        updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.CONFIGURATION_ERROR, message);
    }
}
