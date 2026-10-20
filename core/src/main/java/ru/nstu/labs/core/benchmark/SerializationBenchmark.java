package ru.nstu.labs.core.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import ru.nstu.labs.core.model.ArchivedBatch;
import ru.nstu.labs.core.model.Batch;
import ru.nstu.labs.core.model.ImportedBatch;
import ru.nstu.labs.core.serialization.*;

public class SerializationBenchmark {

  public static String runReport() {
    try {
      int count = 10_000;
      List<Batch> dataset = generateBenchmarkData(count);
      StorageData data = new StorageData(32768, dataset);

      Serializer<StorageData> jsonSer = new JsonBatchSerializer();
      Serializer<StorageData> binSer = new BinaryBatchSerializer();

      Path tempJson = Files.createTempFile("benchmark_dataset_", ".json");
      Path tempBin = Files.createTempFile("benchmark_dataset_", ".bin");

      try {
        BenchmarkResult jsonRes = runBenchmark(jsonSer, data, tempJson, 5);
        BenchmarkResult binRes = runBenchmark(binSer, data, tempBin, 5);

        return formatReport(jsonRes, binRes);
      } finally {
        Files.deleteIfExists(tempJson);
        Files.deleteIfExists(tempBin);
      }
    } catch (Exception e) {
      return "Ошибка бенчмарка: " + e.getMessage();
    }
  }

  public record BenchmarkResult(String format, long fileSizeBytes, double avgSaveMs, double avgLoadMs) {}

  public static BenchmarkResult runBenchmark(
      Serializer<StorageData> serializer, StorageData data, Path file, int runs) throws Exception {
    serializer.save(data, file);
    serializer.load(file);

    long totalSaveNs = 0;
    long totalLoadNs = 0;

    for (int i = 0; i < runs; i++) {
      long t1 = System.nanoTime();
      serializer.save(data, file);
      totalSaveNs += (System.nanoTime() - t1);

      long t2 = System.nanoTime();
      serializer.load(file);
      totalLoadNs += (System.nanoTime() - t2);
    }

    long sizeBytes = Files.size(file);
    return new BenchmarkResult(
        serializer.format().toUpperCase(),
        sizeBytes,
        (totalSaveNs / (double) runs) / 1_000_000.0,
        (totalLoadNs / (double) runs) / 1_000_000.0);
  }

  private static String formatReport(BenchmarkResult json, BenchmarkResult bin) {
    StringBuilder sb = new StringBuilder();
    sb.append("=== СРАВНИТЕЛЬНЫЙ БЕНЧМАРК СЕРИАЛИЗАЦИИ (N = 10 000, 5 прогонов) ===\n\n");
    sb.append(String.format("%-10s | %-16s | %-18s | %-18s\n", "Формат", "Размер файла", "Время сохранения", "Время загрузки"));
    sb.append("-----------------------------------------------------------------------------------\n");
    sb.append(String.format("%-10s | %-12.2f KB | %-15.2f ms | %-15.2f ms\n",
        json.format(), json.fileSizeBytes() / 1024.0, json.avgSaveMs(), json.avgLoadMs()));
    sb.append(String.format("%-10s | %-12.2f KB | %-15.2f ms | %-15.2f ms\n",
        bin.format(), bin.fileSizeBytes() / 1024.0, bin.avgSaveMs(), bin.avgLoadMs()));
    sb.append("-----------------------------------------------------------------------------------\n\n");

    double ratio = (double) json.fileSizeBytes() / bin.fileSizeBytes();
    sb.append(String.format("Вывод: Бинарный формат занимает в %.2f раз меньше места на диске\n", ratio));
    sb.append(String.format("и загружается в %.2f раз быстрее текстового JSON.\n", json.avgLoadMs() / bin.avgLoadMs()));
    return sb.toString();
  }

  private static List<Batch> generateBenchmarkData(int count) {
    List<Batch> list = new ArrayList<>(count);
    LocalDate base = LocalDate.of(2026, 9, 1);
    for (int i = 0; i < count; i++) {
      String sku = String.format("SKU-%06d", i);
      if (i % 5 == 0) {
        list.add(new ArchivedBatch(sku, "Архив номенклатура " + i, 10 + (i % 50), "A-" + (i % 100), base.plusDays(i % 30)));
      } else if (i % 3 == 0) {
        list.add(new ImportedBatch(sku, "Импорт номенклатура " + i, 50 + (i % 200), "B-" + (i % 100), base.plusDays(i % 30), "Китай", "8471607000"));
      } else {
        list.add(new Batch(sku, "Базовая номенклатура " + i, 100 + (i % 500), "C-" + (i % 100), base.plusDays(i % 30)));
      }
    }
    return list;
  }
}
