package ru.nstu.labs.core.serialization;

public enum SerializationErrorCode {
  UNSUPPORTED_VERSION("Неподдерживаемая версия формата"),
  CORRUPTED_FILE("Файл поврежден или имеет неверную структуру"),
  EMPTY_FILE("Файл пуст"),
  DESERIALIZATION_FAILED("Ошибка десериализации объекта"),
  SERIALIZATION_FAILED("Ошибка сериализации объекта");

  private final String description;
  SerializationErrorCode(String description) { this.description = description; }
  public String getDescription() { return description; }
}
