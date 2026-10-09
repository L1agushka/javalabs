package ru.nstu.labs.core.structure;

import ru.nstu.labs.core.model.Batch;

/** Контракт хранилища партий склада на базе собственной структуры данных. */
public interface BatchStorage {
  TableResult<Batch> put(Batch batch);

  TableResult<Batch> get(String sku);

  TableResult<Batch> remove(String sku);

  int size();

  int capacity();

  double loadFactor();

  void clear();

  Batch[] toArray();

  OpenAddressingHashTable.BucketSnapshot<String, Batch>[] getSnapshots();
}
