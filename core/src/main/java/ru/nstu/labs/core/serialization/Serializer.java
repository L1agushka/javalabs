package ru.nstu.labs.core.serialization;

import java.io.IOException;
import java.nio.file.Path;

public interface Serializer<T> {
  String format();
  String description();
  void save(T data, Path file) throws SerializationException, IOException;
  T load(Path file) throws SerializationException, IOException;
}
