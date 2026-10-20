package ru.nstu.labs.core.serialization;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.nstu.labs.core.model.ArchivedBatch;
import ru.nstu.labs.core.model.Batch;
import ru.nstu.labs.core.model.ImportedBatch;

@DisplayName("Тестирование сериализации и десериализации (JSON и Binary)")
class SerializerTest {

  @TempDir
  Path tempDir;

  private JsonBatchSerializer jsonSerializer;
  private BinaryBatchSerializer binarySerializer;
  private StorageData sampleData;

  @BeforeEach
  void setUp() {
    jsonSerializer = new JsonBatchSerializer();
    binarySerializer = new BinaryBatchSerializer();

    Batch b1 = new Batch("SKU-100", "Гайка М8", 500, "A-1", LocalDate.of(2026, 9, 15));
    ArchivedBatch b2 = new ArchivedBatch("SKU-200", "Старый болт", 20, "C-9", LocalDate.of(2024, 1, 10));
    ImportedBatch b3 = new ImportedBatch("SKU-300", "Контроллер", 15, "B-3", LocalDate.of(2026, 9, 20), "Германия", "8504403000");

    sampleData = new StorageData(64, List.of(b1, b2, b3));
  }

  @Test
  @DisplayName("1. Позитивный сценарий: Roundtrip JSON-сериализации load(save(x)) == x")
  void test1_jsonRoundtripPositive() throws Exception {
    Path file = tempDir.resolve("test_roundtrip.json");
    jsonSerializer.save(sampleData, file);

    assertTrue(Files.exists(file));
    assertTrue(Files.size(file) > 0);

    StorageData loaded = jsonSerializer.load(file);
    assertNotNull(loaded);
    assertEquals(sampleData.getVersion(), loaded.getVersion());
    assertEquals(sampleData.getCapacity(), loaded.getCapacity());
    assertEquals(sampleData.getItems().size(), loaded.getItems().size());
    assertEquals(sampleData, loaded);
  }

  @Test
  @DisplayName("2. Позитивный сценарий: Roundtrip Binary-сериализации load(save(x)) == x")
  void test2_binaryRoundtripPositive() throws Exception {
    Path file = tempDir.resolve("test_roundtrip.bin");
    binarySerializer.save(sampleData, file);

    assertTrue(Files.exists(file));
    assertTrue(Files.size(file) > 0);

    StorageData loaded = binarySerializer.load(file);
    assertNotNull(loaded);
    assertEquals(sampleData, loaded);
  }

  @Test
  @DisplayName("3. Позитивный сценарий: Полиморфизм и Read-Only поведение сохраняются после сериализации")
  void test3_polymorphismAndBehaviorPreserved() throws Exception {
    Path file = tempDir.resolve("polymorphism.json");
    jsonSerializer.save(sampleData, file);

    StorageData loaded = jsonSerializer.load(file);
    List<Batch> items = loaded.getItems();

    // Базовая
    assertInstanceOf(Batch.class, items.get(0));
    assertFalse(items.get(0) instanceof ArchivedBatch);
    assertFalse(items.get(0) instanceof ImportedBatch);

    // Архивная
    assertInstanceOf(ArchivedBatch.class, items.get(1));
    ArchivedBatch archived = (ArchivedBatch) items.get(1);
    assertThrows(UnsupportedOperationException.class, () -> archived.setQuantity(999));
    assertThrows(UnsupportedOperationException.class, () -> archived.setName("Новое имя"));

    // Импортная
    assertInstanceOf(ImportedBatch.class, items.get(2));
    ImportedBatch imported = (ImportedBatch) items.get(2);
    assertEquals("Германия", imported.getCountry());
    assertEquals("8504403000", imported.getCustomsCode());
  }

  @Test
  @DisplayName("4. Позитивный сценарий: Чтение эталонного JSON из classpath resources")
  void test4_loadReferenceJsonFromClasspath() throws Exception {
    Path file = copyResourceToTemp("/reference_batches.json", "reference.json");
    StorageData loaded = jsonSerializer.load(file);

    assertNotNull(loaded);
    assertEquals(1, loaded.getVersion());
    assertEquals(16, loaded.getCapacity());
    assertEquals(3, loaded.getItems().size());
    assertEquals("SKU-REF-001", loaded.getItems().get(0).getSku());
    assertInstanceOf(ArchivedBatch.class, loaded.getItems().get(1));
    assertInstanceOf(ImportedBatch.class, loaded.getItems().get(2));
  }

  @Test
  @DisplayName("5. Позитивный сценарий: Генерация и чтение эталонного бинарного файла")
  void test5_binaryReferenceSaveAndLoad() throws Exception {
    Path binFile = tempDir.resolve("ref_test.bin");
    binarySerializer.save(sampleData, binFile);

    StorageData loaded = binarySerializer.load(binFile);
    assertEquals(3, loaded.getItems().size());
    assertEquals("SKU-100", loaded.getItems().get(0).getSku());
  }

  @Test
  @DisplayName("6. Негативный сценарий: Битый синтаксис JSON вызывает SerializationException (CORRUPTED_FILE)")
  void test6_corruptedJsonThrowsSerializationException() throws Exception {
    Path file = copyResourceToTemp("/corrupted_syntax.json", "broken.json");
    SerializationException ex = assertThrows(SerializationException.class, () -> jsonSerializer.load(file));
    assertEquals(SerializationErrorCode.CORRUPTED_FILE, ex.getErrorCode());
  }

  @Test
  @DisplayName("7. Негативный сценарий: Поврежденный бинарный поток вызывает SerializationException (CORRUPTED_FILE)")
  void test7_corruptedBinaryThrowsSerializationException() throws Exception {
    Path file = copyResourceToTemp("/corrupted_binary.bin", "broken.bin");
    SerializationException ex = assertThrows(SerializationException.class, () -> binarySerializer.load(file));
    assertEquals(SerializationErrorCode.CORRUPTED_FILE, ex.getErrorCode());
  }

  @Test
  @DisplayName("8. Негативный сценарий: Неподдерживаемая версия формата вызывает UNSUPPORTED_VERSION")
  void test8_unsupportedVersionThrowsSerializationException() throws Exception {
    Path file = copyResourceToTemp("/unsupported_version.json", "version_999.json");
    SerializationException ex = assertThrows(SerializationException.class, () -> jsonSerializer.load(file));
    assertEquals(SerializationErrorCode.UNSUPPORTED_VERSION, ex.getErrorCode());
  }

  @Test
  @DisplayName("9. Негативный сценарий: Пустой файл (0 байт) вызывает EMPTY_FILE")
  void test9_emptyFileThrowsSerializationException() throws Exception {
    Path emptyFile = tempDir.resolve("truly_empty.json");
    Files.createFile(emptyFile);

    SerializationException exJson = assertThrows(SerializationException.class, () -> jsonSerializer.load(emptyFile));
    assertEquals(SerializationErrorCode.EMPTY_FILE, exJson.getErrorCode());

    SerializationException exBin = assertThrows(SerializationException.class, () -> binarySerializer.load(emptyFile));
    assertEquals(SerializationErrorCode.EMPTY_FILE, exBin.getErrorCode());
  }

  @Test
  @DisplayName("10. Негативный сценарий: Несуществующий файл вызывает FileNotFoundException")
  void test10_missingFileThrowsFileNotFoundException() {
    Path notExisting = tempDir.resolve("does_not_exist_file.json");
    assertThrows(FileNotFoundException.class, () -> jsonSerializer.load(notExisting));
    assertThrows(FileNotFoundException.class, () -> binarySerializer.load(notExisting));
  }

  private Path copyResourceToTemp(String resourceName, String tempName) throws Exception {
    try (InputStream in = getClass().getResourceAsStream(resourceName)) {
      assertNotNull(in, "Ресурс не найден: " + resourceName);
      Path target = tempDir.resolve(tempName);
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      return target;
    }
  }
}
