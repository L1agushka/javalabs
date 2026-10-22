//тут у нас исключение, которое несет один из этих кодов ошибки и сообщение, которое мы сами формируем
//тесты проверяюют по коду , что именно пошло не так, а сообщение для человека, который будет читать лог
//сообщение будет выглядить типо 
//[CORRUPTED_FILE] Файл поврежден, невозможно прочитать данные
//опять таки никакого гениального, просто обертка над IOException с кодом ошибки :))))


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
