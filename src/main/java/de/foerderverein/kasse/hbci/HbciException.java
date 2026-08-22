package de.foerderverein.kasse.hbci;

public class HbciException extends RuntimeException {

    public HbciException(String message) {
        super(message);
    }

    public HbciException(String message, Throwable cause) {
        super(message, cause);
    }
}
