package com.hostdesign24.jobportal.exception;

public class NotificationDeliveryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NotificationDeliveryException(String message) {
        super(message);
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationDeliveryException(Throwable cause) {
        super(cause);
    }
}
