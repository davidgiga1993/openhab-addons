package org.openhab.binding.homematicip.internal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.openhab.binding.homematicip.internal.api.model.HomeState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * Rest client for the homematic IP API
 */
public class HomematicIp {
    /**
     * The base URL of the server which should be used
     * for the current HMIP access point
     */
    private LookupReply lookupReply;

    private final HttpClient client;
    private final ClientCharacteristicsContainer clientCharacteristics;

    private final Map<String, String> headers = new HashMap<>();

    private static final String REST_URL = "https://ps1.homematic.com:6969";
    private static final String LOOKUP_URL = "https://lookup.homematic.com:48335/getHost";

    /**
     * Private client token from the apk
     */
    private static final String clientToken = "jiLpVitHvWnIGD1yo7MA";

    private static final String HEADER_CLIENT_AUTH = "CLIENTAUTH";
    private static final String HEADER_AUTH_TOKEN = "AUTHTOKEN";
    private static final String HEADER_VERSION = "VERSION";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    

    public HomematicIp(HttpClient client, String apId, String accessToken) {
        this.client = client;
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

        if (!client.isStarted()) {
            try {
                client.start();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        String sanitizedApId = apId.replaceAll("[^a-fA-F0-9 ]", "").toUpperCase();
        String tokenHeader = bytesToHex(sha512(sanitizedApId + clientToken));
        headers.put(HEADER_CLIENT_AUTH, tokenHeader);
        headers.put(HEADER_AUTH_TOKEN, accessToken);
        headers.put(HEADER_VERSION, "12");
        headers.put("content-type", CONTENT_TYPE_JSON);
        headers.put("accept", CONTENT_TYPE_JSON);

        clientCharacteristics = new ClientCharacteristicsContainer();
        clientCharacteristics.clientCharacteristics = new ClientCharacteristics();
        clientCharacteristics.id = sanitizedApId;
    }

    public void setTemperature(String groupId, double temperature) throws IOException {
        TemperatureGroupUpdate payload = new TemperatureGroupUpdate(groupId, temperature);
        post("/group/heating/setSetPointTemperature", payload, JsonNode.class);
    }

    public void setSwitchState(String deviceId, int channelIndex, boolean state) throws IOException {
        SwitchStateUpdate payload = new SwitchStateUpdate(deviceId, channelIndex, state);
        post("/device/control/setSwitchState", payload, JsonNode.class);
    }

    public static class SwitchStateUpdate {
        public int channelIndex;
        public String deviceId;
        public boolean on;

        public SwitchStateUpdate(String deviceId, int channelIndex, boolean on) {
            this.channelIndex = channelIndex;
            this.deviceId = deviceId;
            this.on = on;
        }
    }

    public HomeState loadState() throws IOException {
        return post("/home/getCurrentState", clientCharacteristics, HomeState.class);
    }

    /**
     * Queries the urls of the server which should be used for the current HMIP device
     */
    public void lookupUrl() throws IOException {
        lookupReply = post(LOOKUP_URL, clientCharacteristics, LookupReply.class);
    }

    private <T> T post(String path, Object body, Class<T> responseType) throws IOException {
        return post(path, body, responseType, 0);
    }

    private <T> T post(String path, Object body, Class<T> responseType, int currentTry) throws IOException {
        String fullUrl;
        if (path.startsWith("http")) {
            // Absolute url
            fullUrl = path;
        } else {
            fullUrl = lookupReply.restUrl + "/hmip" + path;
        }
        Request request = client.POST(fullUrl);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.header(header.getKey(), header.getValue());
        }

        String jsonStr;
        try {
            jsonStr = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }

        request.content(new StringContentProvider(jsonStr), CONTENT_TYPE_JSON);
        try {
            ContentResponse response = request.send();
            int status = response.getStatus();
            if (status < 200 || status > 299) {
                String error = response.getContentAsString();
                throw new ApiException(status, "Server replied " + status + ": " + error);
            }

            String reply = response.getContentAsString();
            if (reply.length() == 0) {
                return null;
            }

            return mapper.readValue(reply, responseType);
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        } catch (TimeoutException e) {
            // Try again, the backend is not always available
            if (currentTry > 5) {
                // More than 5 tries
                throw new IOException(e);
            }

            return post(path, body, responseType, currentTry + 1);
        }
    }

    private static byte[] sha512(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            return md.digest(content.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static class LookupReply {
        @JsonProperty("urlWebSocket")
        public String webSocketUrl;

        @JsonProperty("urlREST")
        public String restUrl;

        public String apiVersion;
    }

    public static class ClientCharacteristicsContainer {
        public ClientCharacteristics clientCharacteristics;
        public String id;
    }

    public static class ClientCharacteristics {
        public String apiVersion = "10";
        public String applicationIdentifier = "homematicip-python";
        public String applicationVersion = "1.0";
        public String deviceManufacturer = "none";
        public String deviceType = "Computer";
        public String language = "en_US";
        public String osType = "any";
        public String osVersion = "1.0";
    }

    public static class TemperatureGroupUpdate {
        public String groupId;
        public double setPointTemperature;

        public TemperatureGroupUpdate(String groupId, double setPointTemperature) {
            this.groupId = groupId;
            this.setPointTemperature = setPointTemperature;
        }
    }
}
