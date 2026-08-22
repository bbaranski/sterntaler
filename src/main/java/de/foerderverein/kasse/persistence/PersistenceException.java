package de.foerderverein.kasse.persistence;

/** Unchecked Wrapper um SQLException - JDBC-Details sollen nicht bis in die UI-Schicht durchgereicht werden. */
public class PersistenceException extends RuntimeException {

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
