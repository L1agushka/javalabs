package ru.nstu.labs.core.structure;

import java.util.Objects;
import ru.nstu.labs.core.model.Batch;

public class HashTableBatchStorage implements BatchStorage {

  private final OpenAddressingHashTable<String, Batch> table;

  public HashTableBatchStorage(int initialCapacity, double loadFactor) {
    this.table = new OpenAddressingHashTable<>(initialCapacity, loadFactor);
  }

  public HashTableBatchStorage() {
    this.table = new OpenAddressingHashTable<>();
  }

  @Override
  public TableResult<Batch> put(Batch batch) {
    Objects.requireNonNull(batch, "Партия не может быть null");
    return table.put(batch.getSku(), batch);
  }

  @Override
  public TableResult<Batch> get(String sku) {
    return table.get(sku);
  }

  @Override
  public TableResult<Batch> remove(String sku) {
    return table.remove(sku);
  }

  @Override
  public int size() {
    return table.size();
  }

  @Override
  public int capacity() {
    return table.getCapacity();
  }

  @Override
  public double loadFactor() {
    return table.getLoadFactor();
  }

  @Override
  public void clear() {
    table.clear();
  }

  @Override
  public Batch[] toArray() {
    Batch[] result = new Batch[table.size()];
    final int[] cursor = {0};
    table.forEach(
        (sku, batch) -> {
          if (cursor[0] < result.length) {
            result[cursor[0]++] = batch;
          }
        });
    return result;
  }

  @Override
  public OpenAddressingHashTable.BucketSnapshot<String, Batch>[] getSnapshots() {
    return table.getSnapshots();
  }
}
