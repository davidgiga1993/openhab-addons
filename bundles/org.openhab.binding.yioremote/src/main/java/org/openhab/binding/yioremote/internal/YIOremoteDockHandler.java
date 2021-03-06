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
package org.openhab.binding.yioremote.internal;

import static org.openhab.binding.yioremote.internal.YIOremoteBindingConstants.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.yioremote.internal.YIOremoteBindingConstants.YioRemoteDockHandleStatus;
import org.openhab.binding.yioremote.internal.YIOremoteBindingConstants.YioRemoteMessages;
import org.openhab.binding.yioremote.internal.dto.AuthenticationMessage;
import org.openhab.binding.yioremote.internal.dto.IRCode;
import org.openhab.binding.yioremote.internal.dto.IRCodeSendMessage;
import org.openhab.binding.yioremote.internal.dto.IRReceiverMessage;
import org.openhab.binding.yioremote.internal.utils.Websocket;
import org.openhab.binding.yioremote.internal.utils.WebsocketInterface;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link YIOremoteDockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Michael Loercher - Initial contribution
 */
@NonNullByDefault
public class YIOremoteDockHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(YIOremoteDockHandler.class);

    YIOremoteConfiguration localConfig = getConfigAs(YIOremoteConfiguration.class);
    private WebSocketClient webSocketClient = new WebSocketClient();
    private Websocket yioremoteDockwebSocketClient = new Websocket();
    private ClientUpgradeRequest yioremoteDockwebSocketClientrequest = new ClientUpgradeRequest();
    private @Nullable URI websocketAddress;
    private YioRemoteDockHandleStatus yioRemoteDockActualStatus = YioRemoteDockHandleStatus.UNINITIALIZED_STATE;
    private @Nullable Future<?> webSocketPollingJob;
    public String receivedMessage = "";
    private JsonObject recievedJson = new JsonObject();
    private boolean heartBeat = false;
    private boolean authenticationOk = false;
    private String receivedStatus = "";
    private IRCode irCodeReceivedHandler = new IRCode();
    private IRCode irCodeSendHandler = new IRCode();
    private IRCodeSendMessage irCodeSendMessageHandler = new IRCodeSendMessage(irCodeSendHandler);
    private AuthenticationMessage authenticationMessageHandler = new AuthenticationMessage();
    private IRReceiverMessage irReceiverMessageHandler = new IRReceiverMessage();

    public YIOremoteDockHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        scheduler.execute(() -> {
            try {
                websocketAddress = new URI("ws://" + localConfig.host + ":946");
                yioRemoteDockActualStatus = YioRemoteDockHandleStatus.AUTHENTICATION_PROCESS;
            } catch (URISyntaxException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                        "Initialize web socket failed: " + e.getMessage());
            }

            yioremoteDockwebSocketClient.addMessageHandler(new WebsocketInterface() {

                @Override
                public void onConnect(boolean connected) {
                    if (connected) {
                        yioRemoteDockActualStatus = YioRemoteDockHandleStatus.CONNECTION_ESTABLISHED;
                    } else {
                        yioRemoteDockActualStatus = YioRemoteDockHandleStatus.CONNECTION_FAILED;
                    }
                }

                @Override
                public void onMessage(String message) {
                    receivedMessage = message;
                    logger.debug("Message recieved {}", message);
                    recievedJson = convertStringToJsonObject(receivedMessage);
                    if (recievedJson.size() > 0) {
                        if (decodeReceivedMessage(recievedJson)) {
                            triggerChannel(getChannelUuid(GROUP_OUTPUT, STATUS_STRING_CHANNEL));
                            updateChannelString(GROUP_OUTPUT, STATUS_STRING_CHANNEL, receivedStatus);
                            switch (yioRemoteDockActualStatus) {
                                case CONNECTION_ESTABLISHED:
                                case AUTHENTICATION_PROCESS:
                                    authenticate();
                                    break;
                                default:
                                    break;
                            }
                            logger.debug("Message {} decoded", receivedMessage);
                        } else {
                            logger.debug("Error during message {} decoding", receivedMessage);
                        }
                    }
                }

                @Override
                public void onError() {
                    if (webSocketPollingJob != null) {
                        webSocketPollingJob.cancel(true);
                    }
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Connection lost no ping from YIO DOCK");
                    updateState(GROUP_OUTPUT, STATUS_STRING_CHANNEL, UnDefType.UNDEF);
                }
            });

            try {
                webSocketClient.start();

                webSocketClient.connect(yioremoteDockwebSocketClient, websocketAddress,
                        yioremoteDockwebSocketClientrequest);
            } catch (Exception e) {
                logger.debug("Connection error {}", e.getMessage());
            }

        });
    }

    private boolean decodeReceivedMessage(JsonObject message) {
        boolean success = false;

        if (message.has("type")) {
            if (message.get("type").toString().equalsIgnoreCase("\"auth_required\"")) {
                heartBeat = true;
                success = true;
                receivedStatus = "Authentication required";
            } else if (message.get("type").toString().equalsIgnoreCase("\"auth_ok\"")) {
                authenticationOk = true;
                heartBeat = true;
                success = true;
                receivedStatus = "Authentication ok";
            } else if (message.get("type").toString().equalsIgnoreCase("\"dock\"") && message.has("message")) {
                if (message.get("message").toString().equalsIgnoreCase("\"ir_send\"")) {
                    if (message.get("success").toString().equalsIgnoreCase("true")) {
                        receivedStatus = "Send IR Code successfully";
                        heartBeat = true;
                        success = true;
                    } else {
                        if (irCodeSendHandler.getCode().equalsIgnoreCase("\"0;0x0;0;0\"")) {
                            logger.debug("Send heartBeat Code success");
                        } else {
                            receivedStatus = "Send IR Code failure";
                        }
                        heartBeat = true;
                        success = true;
                    }
                } else {
                    logger.warn("No known message {}", receivedMessage);
                    heartBeat = false;
                    success = false;
                }
            } else if (message.get("command").toString().equalsIgnoreCase("\"ir_receive\"")) {
                receivedStatus = message.get("code").toString().replace("\"", "");
                if (receivedStatus.matches("[0-9][;]0[xX][0-9a-fA-F]+[;][0-9]+[;][0-9]")) {
                    irCodeReceivedHandler.setCode(message.get("code").toString().replace("\"", ""));
                } else {
                    irCodeReceivedHandler.setCode("");
                }
                logger.debug("ir_receive message {}", irCodeReceivedHandler.getCode());
                heartBeat = true;
                success = true;
            } else {
                logger.warn("No known message {}", irCodeReceivedHandler.getCode());
                heartBeat = false;
                success = false;
            }
        } else {
            logger.warn("No known message {}", irCodeReceivedHandler.getCode());
            heartBeat = false;
            success = false;
        }
        return success;
    }

    private JsonObject convertStringToJsonObject(String jsonString) {
        try {
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(jsonString);
            JsonObject result;

            if (jsonElement instanceof JsonObject) {
                result = jsonElement.getAsJsonObject();
            } else {
                logger.debug("{} is not valid JSON stirng", jsonString);
                result = new JsonObject();
                throw new IllegalArgumentException(jsonString + "{} is not valid JSON stirng");
            }
            return result;
        } catch (IllegalArgumentException e) {
            JsonObject result = new JsonObject();
            return result;
        }
    }

    public void updateState(String group, String channelId, State value) {
        ChannelUID id = new ChannelUID(getThing().getUID(), group, channelId);
        updateState(id, value);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(YIOremoteDockActions.class);
    }

    @Override
    public void dispose() {
        if (webSocketPollingJob != null) {
            webSocketPollingJob.cancel(true);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (RECEIVER_SWITCH_CHANNEL.equals(channelUID.getIdWithoutGroup())) {
            switch (yioRemoteDockActualStatus) {
                case AUTHENTICATION_COMPLETE:
                    if (command == OnOffType.ON) {
                        logger.debug("YIODOCKRECEIVERSWITCH ON procedure: Switching IR Receiver on");
                        sendMessage(YioRemoteMessages.IR_RECEIVER_ON, "");
                    } else if (command == OnOffType.OFF) {
                        logger.debug("YIODOCKRECEIVERSWITCH OFF procedure: Switching IR Receiver off");
                        sendMessage(YioRemoteMessages.IR_RECEIVER_OFF, "");
                    } else {
                        logger.debug("YIODOCKRECEIVERSWITCH no procedure");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void sendIRCode(@Nullable String irCode) {
        if (irCode != null && yioRemoteDockActualStatus.equals(YioRemoteDockHandleStatus.AUTHENTICATION_COMPLETE)) {
            if (irCode.matches("[0-9][;]0[xX][0-9a-fA-F]+[;][0-9]+[;][0-9]")) {
                sendMessage(YioRemoteMessages.IR_SEND, irCode);
            } else {
                logger.warn("Wrong ir code format {}", irCode);
            }
        }
    }

    private ChannelUID getChannelUuid(String group, String typeId) {
        return new ChannelUID(getThing().getUID(), group, typeId);
    }

    private void updateChannelString(String group, String channelId, String value) {
        ChannelUID id = new ChannelUID(getThing().getUID(), group, channelId);
        updateState(id, new StringType(value));
    }

    private void authenticate() {
        switch (yioRemoteDockActualStatus) {
            case CONNECTION_ESTABLISHED:
                authenticationMessageHandler.setToken(localConfig.accessToken);
                sendMessage(YioRemoteMessages.AUTHENTICATE_MESSAGE, localConfig.accessToken);
                yioRemoteDockActualStatus = YioRemoteDockHandleStatus.AUTHENTICATION_PROCESS;
                break;
            case AUTHENTICATION_PROCESS:
                if (authenticationOk) {
                    yioRemoteDockActualStatus = YioRemoteDockHandleStatus.AUTHENTICATION_COMPLETE;
                    updateStatus(ThingStatus.ONLINE);
                    webSocketPollingJob = scheduler.scheduleWithFixedDelay(this::pollingWebsocket, 0, 30,
                            TimeUnit.SECONDS);
                } else {
                    yioRemoteDockActualStatus = YioRemoteDockHandleStatus.AUTHENTICATION_FAILED;
                }
                break;
            default:
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Connection lost no ping from YIO DOCK");
                updateState(GROUP_OUTPUT, STATUS_STRING_CHANNEL, UnDefType.UNDEF);
                break;
        }
    }

    private void pollingWebsocket() {
        switch (yioRemoteDockActualStatus) {
            case AUTHENTICATION_COMPLETE:
                if (getAndResetHeartbeat()) {
                    updateChannelString(GROUP_OUTPUT, STATUS_STRING_CHANNEL,
                            irCodeReceivedHandler.getCode() + irCodeReceivedHandler.getFormat());
                    logger.debug("heartBeat ok");
                    sendMessage(YioRemoteMessages.HEARTBEAT_MESSAGE, "");
                } else {
                    yioRemoteDockActualStatus = YioRemoteDockHandleStatus.CONNECTION_FAILED;
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Connection lost no ping from YIO DOCK");
                    updateState(GROUP_OUTPUT, STATUS_STRING_CHANNEL, UnDefType.UNDEF);
                    if (webSocketPollingJob != null) {
                        webSocketPollingJob.cancel(true);
                    }
                }
                break;
            default:
                if (webSocketPollingJob != null) {
                    webSocketPollingJob.cancel(true);
                }
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Connection lost no ping from YIO DOCK");
                updateState(GROUP_OUTPUT, STATUS_STRING_CHANNEL, UnDefType.UNDEF);
                break;
        }
    }

    public boolean getAndResetHeartbeat() {
        boolean result = heartBeat;
        heartBeat = false;
        return result;
    }

    public void sendMessage(YioRemoteMessages messageType, String messagePayload) {
        switch (messageType) {
            case AUTHENTICATE_MESSAGE:
                yioremoteDockwebSocketClient.sendMessage(authenticationMessageHandler.getAuthenticationMessageString());
                logger.debug("sending authenticating {}",
                        authenticationMessageHandler.getAuthenticationMessageString());
                break;
            case HEARTBEAT_MESSAGE:
                irCodeSendHandler.setCode("0;0x0;0;0");
                yioremoteDockwebSocketClient.sendMessage(irCodeSendMessageHandler.getIRcodeSendMessageString());
                logger.debug("sending heartBeat message: {}", irCodeSendMessageHandler.getIRcodeSendMessageString());
                break;
            case IR_RECEIVER_ON:
                irReceiverMessageHandler.setOn();
                yioremoteDockwebSocketClient.sendMessage(irReceiverMessageHandler.getIRreceiverMessageString());
                logger.debug("sending IR receiver on message: {}",
                        irReceiverMessageHandler.getIRreceiverMessageString());
                break;
            case IR_RECEIVER_OFF:
                irReceiverMessageHandler.setOff();
                yioremoteDockwebSocketClient.sendMessage(irReceiverMessageHandler.getIRreceiverMessageString());
                logger.debug("sending IR receiver on message: {}",
                        irReceiverMessageHandler.getIRreceiverMessageString());
                break;
            case IR_SEND:
                irCodeSendHandler.setCode(messagePayload);
                yioremoteDockwebSocketClient.sendMessage(irCodeSendMessageHandler.getIRcodeSendMessageString());
                logger.debug("sending heartBeat message: {}", irCodeSendMessageHandler.getIRcodeSendMessageString());
                break;
        }
    }
}
