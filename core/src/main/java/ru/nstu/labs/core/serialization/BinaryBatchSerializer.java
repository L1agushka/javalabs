//реализуем тут Serializer для бинарного форматика через ObjectOutputStream и ObjectInputStream 
//save записывет объект StorageData ПОТОКОМ БАЙтов (WriteObject))
//load проверяет на пустоту как всегда, читает объект и версию чекает 


package ru.nstu.labs.core.serialization;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class BinaryBatchSerializer implements Serializer<StorageData> {
  private static final int MAGIC = 0x4A4C4142; // JLAB

  @Override
  public String format() {
    return "bin";
  }

  @Override
  public String description() {
    return "Бинарный формат Java (Serializable)";
  }

  @Override
  public void save(StorageData data, Path file) throws SerializationException, IOException {
    save(data, file, StorageData.CURRENT_VERSION);
  }

  @Override
  public void save(StorageData data, Path file, int targetVersion)
      throws SerializationException, IOException {
    if (data == null) {
      throw new SerializationException(
          SerializationErrorCode.SERIALIZATION_FAILED, "Данные не могут быть null");
    }
    if (file.getParent() != null) {
      Files.createDirectories(file.getParent());
    }
    try (DataOutputStream header =
        new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
      header.writeInt(MAGIC);
      header.writeInt(targetVersion);
      try (ObjectOutputStream oos = new ObjectOutputStream(header)) {
        oos.writeObject(data.downgradeTo(targetVersion));
      }
    } catch (IOException e) {
      throw new SerializationException(
          SerializationErrorCode.SERIALIZATION_FAILED,
          "Ошибка записи бинарного файла: " + e.getMessage(),
          e);
    }
  }

  @Override
  public StorageData load(Path file) throws SerializationException, IOException {
    if (!Files.exists(file)) {
      throw new FileNotFoundException("Файл не найден: " + file);
    }
    if (Files.size(file) == 0) {
      throw new SerializationException(SerializationErrorCode.EMPTY_FILE, "Файл пуст: " + file);
    }
    try (BufferedInputStream raw = new BufferedInputStream(Files.newInputStream(file))) {
      raw.mark(8);
      DataInputStream header = new DataInputStream(raw);
      int magic = header.readInt();
      int headerVersion;
      if (magic == MAGIC) headerVersion = header.readInt();
      else {
        raw.reset();
        headerVersion = 1;
      }
      try (ObjectInputStream ois = new ObjectInputStream(raw)) {
        Object obj = ois.readObject();
        if (!(obj instanceof StorageData data)) {
          throw new SerializationException(
              SerializationErrorCode.CORRUPTED_FILE,
              "Неожиданный класс в файле: " + (obj != null ? obj.getClass().getName() : "null"));
        }
        if (headerVersion != data.getVersion())
          throw new SerializationException(
              SerializationErrorCode.CORRUPTED_FILE, "Версия в заголовке не совпадает с данными");
        return StorageMigrator.migrate(data);
      }
    } catch (ClassNotFoundException
        | InvalidClassException
        | StreamCorruptedException
        | OptionalDataException e) {
      throw new SerializationException(
          SerializationErrorCode.CORRUPTED_FILE,
          "Бинарный поток поврежден или несовместим: " + e.getMessage(),
          e);
    } catch (EOFException e) {
      throw new SerializationException(
          SerializationErrorCode.CORRUPTED_FILE, "Неожиданный конец бинарного файла", e);
    }
  }

  @Override
  public int readVersion(Path file) throws SerializationException, IOException {
    if (!Files.exists(file)) throw new FileNotFoundException("Файл не найден: " + file);
    if (Files.size(file) < 4)
      throw new SerializationException(
          SerializationErrorCode.CORRUPTED_FILE, "Бинарный файл слишком короткий");
    try (DataInputStream in =
        new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
      int magic = in.readInt();
      if (magic == MAGIC) return in.readInt();
    }
    return 1; // бинарные файлы ЛР3 не имели заголовка
  }
}
