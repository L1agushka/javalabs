//тут мы реализуем наш Serializer для формата JSON через джексон йоу
//сейв создает папки (если их нет пишет сторейдждата в .json файл с отступами)
//лоад проверяет, что файл ряльно существует и он не пустой 
//лоад потом читает JSON в storagedata и чекает версию
//Если джейсончик сломан бросает в CORRUPTED_FILE, если версия не поддерживается бросает в UNSUPPORTED_VERSION, если файл пустой бросает в EMPTY_FILE

package ru.nstu.labs.core.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonBatchSerializer implements Serializer<StorageData> {

  private final ObjectMapper mapper;

  public JsonBatchSerializer() {
    this.mapper = new ObjectMapper();
    this.mapper.registerModule(new JavaTimeModule());
    this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public String format() {
    return "json";
  }

  @Override
  public String description() {
    return "Текстовый формат JSON (Jackson)";
  }

  @Override
  public void save(StorageData data, Path file) throws SerializationException, IOException {
    if (data == null) {
      throw new SerializationException(SerializationErrorCode.SERIALIZATION_FAILED, "Данные не могут быть null");
    }
    if (file.getParent() != null) {
      Files.createDirectories(file.getParent());
    }
    try {
      mapper.writeValue(file.toFile(), data);
    } catch (JsonProcessingException e) {
      throw new SerializationException(
          SerializationErrorCode.SERIALIZATION_FAILED, "Ошибка сериализации JSON: " + e.getMessage(), e);
    }
  }

  @Override
  public StorageData load(Path file) throws SerializationException, IOException {
    if (!Files.exists(file)) {
      throw new FileNotFoundException("Файл не найден: " + file);
    }
    if (Files.size(file) == 0 || Files.readString(file).isBlank()) {
      throw new SerializationException(SerializationErrorCode.EMPTY_FILE, "Файл пуст: " + file);
    }
    try {
      StorageData data = mapper.readValue(file.toFile(), StorageData.class);
      if (data == null) {
        throw new SerializationException(
            SerializationErrorCode.EMPTY_FILE, "Файл не содержит валидного JSON-объекта: " + file);
      }
      if (data.getVersion() > StorageData.CURRENT_VERSION || data.getVersion() <= 0) {
        throw new SerializationException(
            SerializationErrorCode.UNSUPPORTED_VERSION,
            "Версия формата " + data.getVersion() + " не поддерживается (текущая: " + StorageData.CURRENT_VERSION + ")");
      }
      return data;
    } catch (JsonProcessingException e) {
      throw new SerializationException(
          SerializationErrorCode.CORRUPTED_FILE, "Некорректная структура JSON: " + e.getMessage(), e);
    }
  }
}
