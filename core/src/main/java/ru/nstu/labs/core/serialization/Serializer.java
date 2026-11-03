//Возвращает json или bin с помощью format
//возвращает название формата для диалогов description
//записываем данные в файл save
//читаем данные из файла load
//сам интерфейс просто задает правила для двух классов :)



package ru.nstu.labs.core.serialization;

import java.io.IOException;
import java.nio.file.Path;

public interface Serializer<T> {
  String format();

  String description();

  void save(T data, Path file) throws SerializationException, IOException;

  default void save(T data, Path file, int targetVersion)
      throws SerializationException, IOException {
    if (targetVersion != StorageData.CURRENT_VERSION)
      throw new UnsupportedOperationException("Экспорт в указанную версию не поддерживается");
    save(data, file);
  }

  T load(Path file) throws SerializationException, IOException;

  int readVersion(Path file) throws SerializationException, IOException;
}
