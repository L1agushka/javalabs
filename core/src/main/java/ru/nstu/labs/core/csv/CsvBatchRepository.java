//Класс CsvBatchRepository выполняет роль слоя доступа к данным, отвечая за чтение,
//  парсинг, валидацию и сохранение складских партий в формате CSV


package ru.nstu.labs.core.csv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import ru.nstu.labs.core.exception.CsvErrorCode;
import ru.nstu.labs.core.exception.CsvParseException;
import ru.nstu.labs.core.model.ArchivedBatch;
import ru.nstu.labs.core.model.Batch;
import ru.nstu.labs.core.model.ImportedBatch;

public class CsvBatchRepository {



  //Использование record позволяет вернуть из метода load сразу два списка:
  //успешно загруженные объекты и перечень ошибок
  public record LoadResult(List<Batch> items, List<CsvParseException> errors) {}

  private static final String HEADER =
      "TYPE;SKU;NAME;QUANTITY;CELL;DELIVERY_DATE;COUNTRY;CUSTOMS_CODE";

  public LoadResult load(Path file) throws IOException {
    List<Batch> items = new ArrayList<>();
    List<CsvParseException> errors = new ArrayList<>();

    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      String line = reader.readLine();
      if (line == null) {
        errors.add(new CsvParseException(0, CsvErrorCode.EMPTY_FILE, "", "Файл пуст"));
        return new LoadResult(items, errors);
      }



      //Здесь реализовано требование - Битые строки пропускаются
      // Если метод parseLine выбрасывает исключение, программа не падает
      // Ошибка перехватывается, записывается в коллекцию errors, а цикл продолжает читать следующую строку
      int lineNumber = 1;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.isBlank()) {
          continue;
        }

        try {
          Batch parsed = parseLine(line, lineNumber);
          items.add(parsed);
        } catch (CsvParseException ex) {
          errors.add(ex);
        }
      }
    }
    return new LoadResult(items, errors);
  }

  public void save(Path file, List<Batch> items) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      writer.write(HEADER);
      writer.newLine();

      for (Batch batch : items) {
        writer.write(toCsvLine(batch));
        writer.newLine();
      }
    }
  }


  //Используется split(";", -1), чтобы пустые значения в конце строки
  // (например, ;;) не отбрасывались, и массив всегда имел нужную длину. 
  // Бросается собственное исключение с кодом WRONG_FIELD_COUNT
  private Batch parseLine(String line, int lineNumber) throws CsvParseException {
    String[] parts = line.split(";", -1);
    if (parts.length < 8) {
      throw new CsvParseException(
          lineNumber,
          CsvErrorCode.WRONG_FIELD_COUNT,
          line,
          "Ожидалось 8 колонок, получено: " + parts.length);
    }

    String type = parts[0].trim();
    String sku = parts[1].trim();
    String name = parts[2].trim();
    String quantityStr = parts[3].trim();
    String cell = parts[4].trim();
    String dateStr = parts[5].trim();
    String country = parts[6].trim();
    String customsCode = parts[7].trim();



    //Это позволит в GUI показать пользователю, в какой именно строке и колонке он ошибся.
    int quantity;
    try {
      quantity = Integer.parseInt(quantityStr);
    } catch (NumberFormatException e) {
      throw new CsvParseException(
          lineNumber, CsvErrorCode.BAD_NUMBER, line, "Число '" + quantityStr + "' некорректно");
    }

    LocalDate date;
    try {
      date = LocalDate.parse(dateStr);
    } catch (DateTimeParseException e) {
      throw new CsvParseException(
          lineNumber,
          CsvErrorCode.INVALID_DATE,
          line,
          "Дата '" + dateStr + "' не соответствует формату YYYY-MM-DD");
    }

    Batch result =
        switch (type) {
          case "BATCH" -> new Batch(sku, name, quantity, cell, date);
          case "ARCHIVED" -> new ArchivedBatch(sku, name, quantity, cell, date);
          case "IMPORTED" ->
              new ImportedBatch(sku, name, quantity, cell, date, country, customsCode);
          default ->
              throw new CsvParseException(
                  lineNumber,
                  CsvErrorCode.UNKNOWN_TYPE,
                  line,
                  "Тип '" + type + "' не поддерживается");
        };

    List<String> validationErrors = result.validate();
    if (!validationErrors.isEmpty()) {
      throw new CsvParseException(
          lineNumber, CsvErrorCode.VALIDATION_FAILED, line, String.join(", ", validationErrors));
    }

    return result;
  }

  private String toCsvLine(Batch batch) {
    if (batch instanceof ArchivedBatch) {
      return String.join(
          ";",
          "ARCHIVED",
          batch.getSku(),
          batch.getName(),
          String.valueOf(batch.getQuantity()),
          batch.getCell(),
          batch.getDeliveryDate().toString(),
          "",
          "");
    }


    //проверяет реальный тип объекта.
    //  Если это импортная партия, в строку дописываются специфичные поля (страна и код). 
    // Для базовой и архивной партий на месте этих колонок ставятся пустые строки "",
    //  чтобы сохранить единую структуру файла из 8 колонок.
    if (batch instanceof ImportedBatch imported) {
      return String.join(
          ";",
          "IMPORTED",
          imported.getSku(),
          imported.getName(),
          String.valueOf(imported.getQuantity()),
          imported.getCell(),
          imported.getDeliveryDate().toString(),
          imported.getCountry(),
          imported.getCustomsCode());
    }
    return String.join(
        ";",
        "BATCH",
        batch.getSku(),
        batch.getName(),
        String.valueOf(batch.getQuantity()),
        batch.getCell(),
        batch.getDeliveryDate().toString(),
        "",
        "");
  }
}
