package ru.nstu.labs.core.exception;

public class CsvParseException extends Exception {
  private final int lineNumber;
  private final CsvErrorCode errorCode;
  private final String rawLine;

  public CsvParseException(int lineNumber, CsvErrorCode errorCode, String rawLine, String details) {
    super("Строка " + lineNumber + " [" + errorCode.name() + "]: " + details);
    this.lineNumber = lineNumber;
    this.errorCode = errorCode;
    this.rawLine = rawLine;
  }

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
