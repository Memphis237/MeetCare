package org.example.meetcare.exception;

import java.sql.SQLException;

public class DatabaseException extends RuntimeException {
      public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
