//Класс CsvParseException представляет собой пользовательское исключение, которое выбрасывается при ошибках парсинга CSV-файла.


package ru.nstu.labs.core.exception;

public class CsvParseException extends Exception {
  private final int lineNumber;
  private final CsvErrorCode errorCode;
  private final String rawLine;

  //constructor принимает номер строки, код ошибки, исходную строку и подробности ошибки.
  public CsvParseException(int lineNumber, CsvErrorCode errorCode, String rawLine, String details) {
    super("Строка " + lineNumber + " [" + errorCode.name() + "]: " + details);
    this.lineNumber = lineNumber;
    this.errorCode = errorCode;
    this.rawLine = rawLine;
  }


  //возвращают сохраненные значения. Это ключевая часть для связи с графическим интерфейсом
  public int getLineNumber() {
    return lineNumber;
  }

  public CsvErrorCode getErrorCode() {
    return errorCode;
  }

  public String getRawLine() {
    return rawLine;
  }
}
