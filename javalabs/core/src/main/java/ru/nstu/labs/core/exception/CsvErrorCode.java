package ru.nstu.labs.core.exception;

public enum CsvErrorCode {
  WRONG_FIELD_COUNT("Неверное количество колонок в строке"),
  BAD_NUMBER("Некорректный числовой формат количества"),
  INVALID_DATE("Некорректный формат даты (ожидается ГГГГ-ММ-ДД)"),
  UNKNOWN_TYPE("Неизвестный тип сущности партии"),
  VALIDATION_FAILED("Ошибка валидации значений полей"),
  EMPTY_FILE("CSV-файл пуст или не содержит данных");

  private final String description;

  CsvErrorCode(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
