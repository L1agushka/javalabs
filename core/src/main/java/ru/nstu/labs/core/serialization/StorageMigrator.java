package ru.nstu.labs.core.serialization;

import java.time.LocalDate;
import ru.nstu.labs.core.model.Batch;

/** Последовательные, изолированные шаги миграции формата склада. */
public final class StorageMigrator {
  public static final String DEFAULT_NOTE = "Заметка не указана";

  private StorageMigrator() {}

  public static StorageData migrate(StorageData source) throws SerializationException {
    int version = source.getVersion();
    if (version > StorageData.CURRENT_VERSION)
      throw new SerializationException(
          SerializationErrorCode.VERSION_TOO_NEW,
          "Файл версии " + version + " новее версии приложения " + StorageData.CURRENT_VERSION);
    if (version < 1 || source.getCapacity() <= 0)
      throw new SerializationException(
          SerializationErrorCode.MIGRATION_FAILED, "Некорректная версия или ёмкость файла");
    StorageData current = source;
    while (version < StorageData.CURRENT_VERSION) {
      current = version == 1 ? migrateV1toV2(current) : migrateV2toV3(current);
      version++;
    }
    return current;
  }

  static StorageData migrateV1toV2(StorageData data) throws SerializationException {
    for (Batch batch : data.getItems()) {
      if (batch.getSku() == null || batch.getSku().isBlank())
        throw new SerializationException(
            SerializationErrorCode.MIGRATION_FAILED, "Невозможно мигрировать партию без артикула");
      batch.applyMigrationDefaults(DEFAULT_NOTE, null);
    }
    return new StorageData(2, data.getCapacity(), data.getItems());
  }

  static StorageData migrateV2toV3(StorageData data) {
    for (Batch batch : data.getItems())
      batch.applyMigrationDefaults(
          null,
          batch.getDeliveryDate() != null ? batch.getDeliveryDate() : LocalDate.of(1970, 1, 1));
    return new StorageData(3, data.getCapacity(), data.getItems());
  }
}
