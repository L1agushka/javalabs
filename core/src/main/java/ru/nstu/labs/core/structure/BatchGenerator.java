package ru.nstu.labs.core.structure;

import java.time.LocalDate;
import java.util.Random;
import java.util.function.Consumer;
import ru.nstu.labs.core.model.ArchivedBatch;
import ru.nstu.labs.core.model.Batch;
import ru.nstu.labs.core.model.ImportedBatch;

public final class BatchGenerator {

  private static final String[] PRODUCTS = {
    "Ноутбук", "Монитор", "Клавиатура", "Мышь", "SSD-накопитель",
    "Видеокарта", "Оперативная память", "Маршрутизатор", "Серверная стойка", "Кабель Ethernet"
  };

  private static final String[] COUNTRIES = {
    "Китай", "Германия", "Тайвань", "Южная Корея", "Вьетнам", "Япония"
  };

  private BatchGenerator() {}

  /**
   * Генерирует N партий и передает их в consumer. При сигнале отмены (Thread.interrupted)
   * немедленно прерывает генерацию.
   */
  public static int generate(int count, Consumer<Batch> consumer) throws InterruptedException {
    Random random = new Random();
    int created = 0;

    for (int i = 1; i <= count; i++) {
      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException("Генерация отменена пользователем");
      }

      String sku = String.format("SKU-%06d", i);
      String name = PRODUCTS[random.nextInt(PRODUCTS.length)] + " #" + (100 + random.nextInt(900));
      int qty = 1 + random.nextInt(500);
      String cell =
          String.format("%c-%02d", (char) ('A' + random.nextInt(6)), 1 + random.nextInt(50));
      LocalDate date = LocalDate.now().minusDays(random.nextInt(365));

      int typeSelector = random.nextInt(100);
      Batch batch;

      if (typeSelector < 20) {
        // 20% архивных
        batch = new ArchivedBatch(sku, name, qty, cell, date);
      } else if (typeSelector < 50) {
        // 30% импортных
        String country = COUNTRIES[random.nextInt(COUNTRIES.length)];
        String customsCode = String.format("%010d", 8471000000L + random.nextInt(999999));
        batch = new ImportedBatch(sku, name, qty, cell, date, country, customsCode);
      } else {
        // 50% базовых
        batch = new Batch(sku, name, qty, cell, date);
      }

      consumer.accept(batch);
      created++;
    }

    return created;
  }
}
