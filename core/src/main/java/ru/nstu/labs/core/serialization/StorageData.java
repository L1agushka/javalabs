package ru.nstu.labs.core.serialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import ru.nstu.labs.core.model.Batch;

public class StorageData implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final int CURRENT_VERSION = 1;

  private final int version;
  private final int capacity;
  private final List<Batch> items;

  @JsonCreator
  public StorageData(
      @JsonProperty("version") int version,
      @JsonProperty("capacity") int capacity,
      @JsonProperty("items") List<Batch> items) {
    this.version = version;
    this.capacity = capacity;
    this.items = items != null ? Collections.unmodifiableList(new ArrayList<>(items)) : List.of();
  }

  public StorageData(int capacity, List<Batch> items) {
    this(CURRENT_VERSION, capacity, items);
  }

  public int getVersion() { return version; }
  public int getCapacity() { return capacity; }
  public List<Batch> getItems() { return items; }

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
      if (b1.getClass() != b2.getClass()) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, capacity, items.size());
  }
}
