package ru.nstu.labs.core.serialization;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.nstu.labs.core.model.Batch;

class StorageMigrationTest {
  @TempDir Path tempDir;

  @Test
  void migratesV1ToV3AndFillsNonNullDefaults() throws Exception {
    StorageData result =
        StorageMigrator.migrate(
            new StorageData(
                1, 16, List.of(new Batch("A-1", "Крепёж", 4, "A-1", LocalDate.of(2026, 1, 2)))));
    Batch batch = result.getItems().getFirst();
    assertEquals(3, result.getVersion());
    assertEquals(StorageMigrator.DEFAULT_NOTE, batch.getNote());
    assertEquals(LocalDate.of(2026, 1, 2), batch.getLastUpdated());
  }

  @Test
  void migratesV2ToV3WithoutReplacingExistingNote() throws Exception {
    Batch batch = new Batch("A-2", "Кабель", 2, "B-1", LocalDate.of(2026, 2, 3), "Проверено", null);
    StorageData result = StorageMigrator.migrate(new StorageData(2, 16, List.of(batch)));
    assertEquals("Проверено", result.getItems().getFirst().getNote());
    assertNotNull(result.getItems().getFirst().getLastUpdated());
  }

  @Test
  void exportsV1ThenLoadsItBackToCurrentSchema() throws Exception {
    JsonBatchSerializer serializer = new JsonBatchSerializer();
    Path file = tempDir.resolve("warehouse-v1.json");
    serializer.save(
        new StorageData(
            16,
            List.of(
                new Batch("A-3", "Гайка", 1, "A-2", LocalDate.now(), "Важно", LocalDate.now()))),
        file,
        1);
    assertEquals(1, serializer.readVersion(file));
    StorageData migrated = serializer.load(file);
    assertEquals(3, migrated.getVersion());
    assertEquals(StorageMigrator.DEFAULT_NOTE, migrated.getItems().getFirst().getNote());
  }

  @Test
  void binaryHeaderContainsRequestedVersionAndMigrates() throws Exception {
    BinaryBatchSerializer serializer = new BinaryBatchSerializer();
    Path file = tempDir.resolve("warehouse-v2.bin");
    serializer.save(
        new StorageData(16, List.of(new Batch("A-4", "Болт", 1, "A-3", LocalDate.now()))), file, 2);
    assertEquals(2, serializer.readVersion(file));
    assertEquals(3, serializer.load(file).getVersion());
  }

  @Test
  void refusesFileFromFutureVersion() {
    SerializationException error =
        assertThrows(
            SerializationException.class,
            () -> StorageMigrator.migrate(new StorageData(4, 16, List.of())));
    assertEquals(SerializationErrorCode.VERSION_TOO_NEW, error.getErrorCode());
  }

  @Test
  void rejectsInvalidCapacityDuringMigration() {
    SerializationException error =
        assertThrows(
            SerializationException.class,
            () -> StorageMigrator.migrate(new StorageData(1, 0, List.of())));
    assertEquals(SerializationErrorCode.MIGRATION_FAILED, error.getErrorCode());
  }
}
