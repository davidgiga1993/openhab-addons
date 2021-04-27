package org.openhab.binding.homematicip.internal.api;

import java.io.IOException;

public class ApiException extends IOException {
    public int statusCode;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
