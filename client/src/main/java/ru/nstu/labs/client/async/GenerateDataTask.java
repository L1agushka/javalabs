package ru.nstu.labs.client.async;

import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Task;
import ru.nstu.labs.core.model.Batch;
import ru.nstu.labs.core.structure.BatchGenerator;
import ru.nstu.labs.core.structure.BatchStorage;

public class GenerateDataTask extends Task<List<Batch>> {

  private final int totalCount;
  private final BatchStorage storage;

  public GenerateDataTask(int totalCount, BatchStorage storage) {
    this.totalCount = totalCount;
    this.storage = storage;
  }

  @Override
  protected List<Batch> call() throws Exception {
    List<Batch> generatedList = new ArrayList<>(totalCount);
    int[] counter = {0};

    BatchGenerator.generate(
        totalCount,
        batch -> {
          if (isCancelled()) {
            throw new RuntimeException("Операция отменена");
          }

          storage.put(batch);
          generatedList.add(batch);
          counter[0]++;

          // Обновляем прогресс каждые 10 элементов или на финише
          if (counter[0] % 10 == 0 || counter[0] == totalCount) {
            updateProgress(counter[0], totalCount);
            updateMessage(String.format("Сгенерировано %d из %d...", counter[0], totalCount));
          }
        });

    updateMessage("Генерация успешно завершена");
    return generatedList;
  }
}
