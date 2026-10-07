package ru.nstu.labs.core.structure;

import java.util.function.BiConsumer;

/**
 * Потокобезопасная обертка для OpenAddressingHashTable. Все операции чтения и мутации
 * синхронизированы через монитор объекта.
 */
public class SynchronizedHashTable<K, V> {

  private final OpenAddressingHashTable<K, V> table;

  public SynchronizedHashTable(int initialCapacity, double loadFactor) {
    this.table = new OpenAddressingHashTable<>(initialCapacity, loadFactor);
  }

  public SynchronizedHashTable() {
    this.table = new OpenAddressingHashTable<>();
  }

  public synchronized TableResult<V> put(K key, V value) {
    return table.put(key, value);
  }

  public synchronized TableResult<V> get(K key) {
    return table.get(key);
  }

  public synchronized TableResult<V> remove(K key) {
    return table.remove(key);
  }

  public synchronized int size() {
    return table.size();
  }

  public synchronized int getCapacity() {
    return table.getCapacity();
  }

  public synchronized double getLoadFactor() {
    return table.getLoadFactor();
  }

  public synchronized void clear() {
    table.clear();
  }

  public synchronized void forEach(BiConsumer<K, V> action) {
    table.forEach(action);
  }

  public synchronized OpenAddressingHashTable.BucketSnapshot<K, V>[] getSnapshots() {
    return table.getSnapshots();
  }
}
