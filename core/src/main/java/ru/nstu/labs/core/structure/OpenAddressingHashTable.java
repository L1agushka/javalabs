package ru.nstu.labs.core.structure;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Хеш-таблица с открытой адресацией и линейным пробированием. Реализована на сырых массивах без
 * использования коллекций java.util.
 */
public class OpenAddressingHashTable<K, V> {

  public enum NodeState {
    EMPTY, // Ячейка никогда не была занята
    OCCUPIED, // Ячейка содержит актуальные данные
    DELETED // Tombstone: элемент удален, цепочка пробирования не прерывается
  }

  /** Снимок одной ячейки для визуализатора Canvas. */
  public record BucketSnapshot<K, V>(int index, NodeState state, K key, V value) {}

  private static final int DEFAULT_CAPACITY = 16;
  private static final double DEFAULT_LOAD_FACTOR = 0.75;

  private K[] keys;
  private V[] values;
  private NodeState[] states;

  private int capacity;
  private final double loadFactorThreshold;
  private int size; // Число реально занятых ячеек (OCCUPIED)
  private int deletedCount; // Число удаленных ячеек (DELETED)

  @SuppressWarnings("unchecked")  //Java, успокойся. Я знаю, что делаю, этот код написан осознанно. Выключи для этого конструктора жёлтые предупреждения и не ругайся
  public OpenAddressingHashTable(int initialCapacity, double loadFactor) {
    if (initialCapacity <= 0) {
      throw new IllegalArgumentException("Емкость должна быть положительной");
    }
    this.capacity = initialCapacity;
    this.loadFactorThreshold = loadFactor;
    this.keys = (K[]) new Object[capacity];
    this.values = (V[]) new Object[capacity];
    this.states = new NodeState[capacity];
    Arrays.fill(this.states, NodeState.EMPTY);
    this.size = 0;
    this.deletedCount = 0;
  }

  public OpenAddressingHashTable() {
    this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
  }

  /** Первичная хеш-функция, отображающая ключ в индекс массива. */
  private int hash(K key) {
    if (key == null) return 0;
    return (key.hashCode() & 0x7fffffff) % capacity;
  }

  /** Вставка / обновление по ключу. */
  public TableResult<V> put(K key, V value) {
    Objects.requireNonNull(key, "Ключ не может быть null");

    if ((double) (size + deletedCount + 1) / capacity >= loadFactorThreshold) {
      rehash(capacity * 2);
    }

    int startIdx = hash(key);
    int[] visited = new int[capacity];
    int probes = 0;

    int firstDeletedIdx = -1;
    int targetIdx = -1;

    for (int i = 0; i < capacity; i++) {
      int idx = (startIdx + i) % capacity;
      visited[probes++] = idx;

      if (states[idx] == NodeState.OCCUPIED) {
        if (Objects.equals(keys[idx], key)) {
          values[idx] = value;
          return new TableResult<>(value, true, probes, Arrays.copyOf(visited, probes));
        }
      } else if (states[idx] == NodeState.DELETED) {
        if (firstDeletedIdx == -1) {
          firstDeletedIdx = idx;
        }
      } else if (states[idx] == NodeState.EMPTY) {
        targetIdx = (firstDeletedIdx != -1) ? firstDeletedIdx : idx;
        break;
      }
    }

    if (targetIdx == -1 && firstDeletedIdx != -1) {
      targetIdx = firstDeletedIdx;
    }

    if (targetIdx != -1) {
      if (states[targetIdx] == NodeState.DELETED) {
        deletedCount--;
      }
      keys[targetIdx] = key;
      values[targetIdx] = value;
      states[targetIdx] = NodeState.OCCUPIED;
      size++;
      return new TableResult<>(value, true, probes, Arrays.copyOf(visited, probes));
    }

    rehash(capacity * 2);
    return put(key, value);
  }

  /** Поиск значения по ключу со сбором статистики проб. */
  public TableResult<V> get(K key) {
    if (key == null || size == 0) {
      return new TableResult<>(null, false, 0, new int[0]);
    }

    int startIdx = hash(key);
    int[] visited = new int[capacity];
    int probes = 0;

    for (int i = 0; i < capacity; i++) {
      int idx = (startIdx + i) % capacity;
      visited[probes++] = idx;

      if (states[idx] == NodeState.EMPTY) {
        return new TableResult<>(null, false, probes, Arrays.copyOf(visited, probes));
      }

      if (states[idx] == NodeState.OCCUPIED && Objects.equals(keys[idx], key)) {
        return new TableResult<>(values[idx], true, probes, Arrays.copyOf(visited, probes));
      }
    }

    return new TableResult<>(null, false, probes, Arrays.copyOf(visited, probes));
  }

  /** Удаление по ключу с выставлением статуса DELETED (tombstone). */
  public TableResult<V> remove(K key) {
    if (key == null || size == 0) {
      return new TableResult<>(null, false, 0, new int[0]);
    }

    int startIdx = hash(key);
    int[] visited = new int[capacity];
    int probes = 0;

    for (int i = 0; i < capacity; i++) {
      int idx = (startIdx + i) % capacity;
      visited[probes++] = idx;

      if (states[idx] == NodeState.EMPTY) {
        return new TableResult<>(null, false, probes, Arrays.copyOf(visited, probes));
      }

      if (states[idx] == NodeState.OCCUPIED && Objects.equals(keys[idx], key)) {
        V oldValue = values[idx];
        keys[idx] = null;
        values[idx] = null;
        states[idx] = NodeState.DELETED;
        size--;
        deletedCount++;
        return new TableResult<>(oldValue, true, probes, Arrays.copyOf(visited, probes));
      }
    }

    return new TableResult<>(null, false, probes, Arrays.copyOf(visited, probes));
  }

  /** Автоматический рехеш: удвоение массива и пересчет позиций всех элементов. */
  @SuppressWarnings("unchecked")
  private void rehash(int newCapacity) {
    K[] oldKeys = this.keys;
    V[] oldValues = this.values;
    NodeState[] oldStates = this.states;
    int oldCap = this.capacity;

    this.capacity = newCapacity;
    this.keys = (K[]) new Object[newCapacity];
    this.values = (V[]) new Object[newCapacity];
    this.states = new NodeState[newCapacity];
    Arrays.fill(this.states, NodeState.EMPTY);
    this.size = 0;
    this.deletedCount = 0;

    for (int i = 0; i < oldCap; i++) {
      if (oldStates[i] == NodeState.OCCUPIED) {
        internalPut(oldKeys[i], oldValues[i]);
      }
    }
  }

  private void internalPut(K key, V value) {
    int startIdx = hash(key);
    for (int i = 0; i < capacity; i++) {
      int idx = (startIdx + i) % capacity;
      if (states[idx] == NodeState.EMPTY) {
        keys[idx] = key;
        values[idx] = value;
        states[idx] = NodeState.OCCUPIED;
        size++;
        return;
      }
    }
  }

  public int size() {
    return size;
  }

  public int getCapacity() {
    return capacity;
  }

  public double getLoadFactor() {
    return (double) size / capacity;
  }

  /** Итерация по всем элементам без использования java.util.List. */
  public void forEach(BiConsumer<K, V> action) {
    for (int i = 0; i < capacity; i++) {
      if (states[i] == NodeState.OCCUPIED) {
        action.accept(keys[i], values[i]);
      }
    }
  }

  /** Экспорт снимка всех ячеек для графической отрисовки в Canvas. */
  @SuppressWarnings("unchecked")
  public BucketSnapshot<K, V>[] getSnapshots() {
    BucketSnapshot<K, V>[] snapshots = new BucketSnapshot[capacity];
    for (int i = 0; i < capacity; i++) {
      snapshots[i] = new BucketSnapshot<>(i, states[i], keys[i], values[i]);
    }
    return snapshots;
  }

  public void clear() {
    Arrays.fill(keys, null);
    Arrays.fill(values, null);
    Arrays.fill(states, NodeState.EMPTY);
    size = 0;
    deletedCount = 0;
  }
}
