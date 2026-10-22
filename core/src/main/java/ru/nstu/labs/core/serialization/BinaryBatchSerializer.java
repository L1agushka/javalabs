//реализуем тут Serializer для бинарного форматика через ObjectOutputStream и ObjectInputStream 
//save записывет объект StorageData ПОТОКОМ БАЙтов (WriteObject))
//load проверяет на пустоту как всегда, читает объект и версию чекает 


package ru.nstu.labs.core.serialization;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class BinaryBatchSerializer implements Serializer<StorageData> {

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
    if (data == null) {
      throw new SerializationException(SerializationErrorCode.SERIALIZATION_FAILED, "Данные не могут быть null");
    }
    if (file.getParent() != null) {
      Files.createDirectories(file.getParent());
    }
    try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
      oos.writeObject(data);
    } catch (IOException e) {
      throw new SerializationException(
          SerializationErrorCode.SERIALIZATION_FAILED, "Ошибка записи бинарного файла: " + e.getMessage(), e);
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
    try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
      Object obj = ois.readObject();
      if (!(obj instanceof StorageData data)) {
        throw new SerializationException(
            SerializationErrorCode.CORRUPTED_FILE,
            "Неожиданный класс в файле: " + (obj != null ? obj.getClass().getName() : "null"));
      }
      if (data.getVersion() > StorageData.CURRENT_VERSION || data.getVersion() <= 0) {
        throw new SerializationException(
            SerializationErrorCode.UNSUPPORTED_VERSION,
            "Версия формата " + data.getVersion() + " не поддерживается (текущая: " + StorageData.CURRENT_VERSION + ")");
      }
      return data;
    } catch (ClassNotFoundException | InvalidClassException | StreamCorruptedException | OptionalDataException e) {
      throw new SerializationException(
          SerializationErrorCode.CORRUPTED_FILE, "Бинарный поток поврежден или несовместим: " + e.getMessage(), e);
    } catch (EOFException e) {
      throw new SerializationException(SerializationErrorCode.CORRUPTED_FILE, "Неожиданный конец бинарного файла", e);
    }
  }
}
