package ru.nstu.labs.core.serialization;

import java.io.IOException;

public class SerializationException extends IOException {
  private final SerializationErrorCode errorCode;

  public SerializationException(SerializationErrorCode errorCode, String message) {
    super("[" + errorCode.name() + "] " + message);
    this.errorCode = errorCode;
  }

  public SerializationException(SerializationErrorCode errorCode, String message, Throwable cause) {
    super("[" + errorCode.name() + "] " + message, cause);
    this.errorCode = errorCode;
  }

  public SerializationErrorCode getErrorCode() {
    return errorCode;
  }
}
