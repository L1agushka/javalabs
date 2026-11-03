//некая посылка которая записывается в файл и в ней три поля
//версион (версия), капасити (емкость хеш), айтемс (списсок партий)
//Экспорт получается упаковывает в этот объект, импорт распаковывает
//
// :)

//кратенько для себя напишу 
//Эскпорт
//наша кнопочка в GUI -> StorageData Собирается из таблички -> serializer.save() в JSON или BIN -> записываем в файл
//импорт
//наша кнопочка в GUI -> serializer.load() из JSON или BIN -> StorageData распаковывается -> StorageData в табличку
//ВОТ И ВЕСЬ СЕКРЕТИК 


package ru.nstu.labs.core.serialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import ru.nstu.labs.core.model.ArchivedBatch;
import ru.nstu.labs.core.model.Batch;
import ru.nstu.labs.core.model.ImportedBatch;

public class StorageData implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  public static final int CURRENT_VERSION = 3;

  private final int version;
  private final int capacity;
  private final List<Batch> items;

  @JsonCreator
  public StorageData(
      @JsonProperty("schema_version") Integer schemaVersion,
      @JsonProperty("version") Integer legacyVersion,
      @JsonProperty("capacity") int capacity,
      @JsonProperty("items") List<Batch> items) {
    this.version =
        schemaVersion != null ? schemaVersion : (legacyVersion != null ? legacyVersion : 1);
    this.capacity = capacity;
    this.items = items != null ? Collections.unmodifiableList(new ArrayList<>(items)) : List.of();
  }

  public StorageData(int version, int capacity, List<Batch> items) {
    this(Integer.valueOf(version), null, capacity, items);
  }

  public StorageData(int capacity, List<Batch> items) {
    this(CURRENT_VERSION, capacity, items);
  }

  @JsonIgnore
  public int getVersion() {
    return version;
  }

  @JsonProperty("schema_version")
  public int getSchemaVersion() {
    return version;
  }

  public int getCapacity() {
    return capacity;
  }

  public List<Batch> getItems() {
    return items;
  }

  /** Формирует совместимый снимок для старой схемы с осознанной потерей новых полей. */
  public StorageData downgradeTo(int targetVersion) {
    if (targetVersion < 1 || targetVersion > CURRENT_VERSION)
      throw new IllegalArgumentException("Неизвестная версия схемы: " + targetVersion);
    List<Batch> copy = new ArrayList<>(items.size());
    for (Batch batch : items) copy.add(copyForVersion(batch, targetVersion));
    return new StorageData(targetVersion, capacity, copy);
  }

  private static Batch copyForVersion(Batch batch, int version) {
    String note = version >= 2 ? batch.getNote() : null;
    LocalDate lastUpdated = version >= 3 ? batch.getLastUpdated() : null;
    if (batch instanceof ImportedBatch imported)
      return new ImportedBatch(
          batch.getSku(),
          batch.getName(),
          batch.getQuantity(),
          batch.getCell(),
          batch.getDeliveryDate(),
          imported.getCountry(),
          imported.getCustomsCode(),
          note,
          lastUpdated);
    if (batch instanceof ArchivedBatch)
      return new ArchivedBatch(
          batch.getSku(),
          batch.getName(),
          batch.getQuantity(),
          batch.getCell(),
          batch.getDeliveryDate(),
          note,
          lastUpdated);
    return new Batch(
        batch.getSku(),
        batch.getName(),
        batch.getQuantity(),
        batch.getCell(),
        batch.getDeliveryDate(),
        note,
        lastUpdated);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StorageData that = (StorageData) o;
    if (version != that.version || capacity != that.capacity) return false;
    if (items.size() != that.items.size()) return false;
    for (int i = 0; i < items.size(); i++) {
      Batch b1 = items.get(i);
      Batch b2 = that.items.get(i);
      if (!Objects.equals(b1.getSku(), b2.getSku())) return false;
      if (!Objects.equals(b1.getName(), b2.getName())) return false;
      if (b1.getQuantity() != b2.getQuantity()) return false;
      if (!Objects.equals(b1.getCell(), b2.getCell())) return false;
      if (!Objects.equals(b1.getDeliveryDate(), b2.getDeliveryDate())) return false;
      if (!Objects.equals(b1.getNote(), b2.getNote())) return false;
      if (!Objects.equals(b1.getLastUpdated(), b2.getLastUpdated())) return false;
      if (b1.getClass() != b2.getClass()) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, capacity, items.size());
  }
}
