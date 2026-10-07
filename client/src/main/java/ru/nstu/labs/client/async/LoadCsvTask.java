package ru.nstu.labs.client.async;

import java.io.File;
import javafx.concurrent.Task;
import ru.nstu.labs.core.csv.CsvBatchRepository;

public class LoadCsvTask extends Task<CsvBatchRepository.LoadResult> {

  private final File file;
  private final CsvBatchRepository repository;

  public LoadCsvTask(File file, CsvBatchRepository repository) {
    this.file = file;
    this.repository = repository;
  }

  @Override
  protected CsvBatchRepository.LoadResult call() throws Exception {
    updateMessage("Чтение CSV-файла...");
    updateProgress(0, 100);

    CsvBatchRepository.LoadResult result =
        repository.load(
            file.toPath(),
            (current, total) -> {
              if (isCancelled()) {
                throw new RuntimeException("Загрузка отменена пользователем");
              }
              updateProgress(current, total);
              updateMessage(String.format("Загрузка: %d / %d строк", current, total));
            });

    updateMessage("Файл успешно прочитан");
    return result;
  }
}
