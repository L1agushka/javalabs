package ru.nstu.labs.core.structure;

/**
 * Контейнер результата операции хеш-таблицы с метриками пробирования.
 *
 * @param value найденное/удаленное/вставленное значение (или null)
 * @param found флаг успешности поиска/удаления
 * @param probesCount число проверенных ячеек (проб)
 * @param visitedIndices массив индексов ячеек, посещенных в ходе линейного пробирования
 */
public record TableResult<V>(V value, boolean found, int probesCount, int[] visitedIndices) {}
